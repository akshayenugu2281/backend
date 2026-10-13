package com.library360.support;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @RequestMapping("/api/v1/support") @RequiredArgsConstructor
public class SupportController {
  private final JdbcTemplate jdbc;
  public record CreateConversation(String subject, String message) {}
  public record SendMessage(String body) {}
  public record Conversation(long id,long requesterUserId,String requesterName,String requesterEmail,String subject,String status,String updatedAt,long unreadCount) {}
  public record Message(long id,long conversationId,long senderUserId,String senderName,String senderRole,String body,String createdAt) {}

  @GetMapping("/conversations")
  public List<Conversation> conversations(@AuthenticationPrincipal Jwt jwt) {
    long uid=userId(jwt); boolean staff=isStaff(jwt);
    String sql="SELECT c.id,c.requester_user_id,u.full_name requester_name,u.email requester_email,c.subject,c.status,c.updated_at,(SELECT COUNT(*) FROM support_messages m WHERE m.conversation_id=c.id AND m.sender_user_id<>? AND m.read_at IS NULL) unread_count FROM support_conversations c JOIN users u ON u.id=c.requester_user_id "+(staff?"":"WHERE c.requester_user_id=? ")+"ORDER BY c.updated_at DESC LIMIT 200";
    if(staff)return jdbc.query(sql,(rs,i)->conv(rsMap(rs)),uid);
    return jdbc.query(sql,(rs,i)->new Conversation(rs.getLong("id"),rs.getLong("requester_user_id"),rs.getString("requester_name"),rs.getString("requester_email"),rs.getString("subject"),rs.getString("status"),rs.getTimestamp("updated_at").toInstant().toString(),rs.getLong("unread_count")),uid,uid);
  }

  @PostMapping("/conversations") @Transactional
  public Conversation create(@AuthenticationPrincipal Jwt jwt,@RequestBody CreateConversation req) {
    if(isStaff(jwt))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Staff should reply to an existing conversation or direct students to open a request.");
    String subject=clean(req.subject()),body=clean(req.message());
    if(subject==null||subject.length()>160||body==null||body.length()>3000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Subject (1–160 characters) and message (1–3000 characters) are required.");
    long uid=userId(jwt); jdbc.update("INSERT INTO support_conversations(requester_user_id,subject,status,created_at,updated_at) VALUES(?,?,'OPEN',NOW(6),NOW(6))",uid,subject);
    long id=jdbc.queryForObject("SELECT LAST_INSERT_ID()",Long.class);
    jdbc.update("INSERT INTO support_messages(conversation_id,sender_user_id,body,created_at) VALUES(?,?,?,NOW(6))",id,uid,body);
    notifyLibraryTeam("New library support request", "A college member opened a support request: " + subject + ". Open Library support to reply.");
    return getConversation(jwt,id);
  }

  @GetMapping("/conversations/{id}/messages")
  @Transactional
  public List<Message> messages(@AuthenticationPrincipal Jwt jwt,@PathVariable long id) {
    assertAccess(jwt,id);
    jdbc.update("UPDATE support_messages SET read_at=NOW(6) WHERE conversation_id=? AND sender_user_id<>? AND read_at IS NULL",id,userId(jwt));
    return jdbc.query("SELECT DISTINCT m.id,m.conversation_id,m.sender_user_id,u.full_name,u.email,m.body,m.created_at,(SELECT r.name FROM user_roles ur JOIN roles r ON r.id=ur.role_id WHERE ur.user_id=u.id ORDER BY r.name ASC LIMIT 1) role_name FROM support_messages m JOIN users u ON u.id=m.sender_user_id WHERE m.conversation_id=? ORDER BY m.created_at ASC, m.id ASC LIMIT 500",
      (rs,i)->new Message(rs.getLong("id"),rs.getLong("conversation_id"),rs.getLong("sender_user_id"),rs.getString("full_name"),rs.getString("role_name"),rs.getString("body"),rs.getTimestamp("created_at").toInstant().toString()),id);
  }

  @PostMapping("/conversations/{id}/messages") @Transactional
  public Message send(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@RequestBody SendMessage req) {
    assertAccess(jwt,id); String body=clean(req.body()); if(body==null||body.length()>3000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Message must contain 1–3000 characters.");
    String status=jdbc.queryForObject("SELECT status FROM support_conversations WHERE id=?",String.class,id);
    if("CLOSED".equals(status))throw new ResponseStatusException(HttpStatus.CONFLICT,"This conversation is closed. Open a new support request if you need more help.");
    long uid=userId(jwt); jdbc.update("INSERT INTO support_messages(conversation_id,sender_user_id,body,created_at) VALUES(?,?,?,NOW(6))",id,uid,body);
    if(isStaff(jwt)) {
      jdbc.update("UPDATE support_conversations SET assigned_staff_id=COALESCE(assigned_staff_id,?),updated_at=NOW(6) WHERE id=?",uid,id);
      jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,`read`,created_at) SELECT requester_user_id,'SUPPORT_REPLY','Library team replied',CONCAT('A reply was added to your support request: ',subject),'/support',FALSE,NOW(6) FROM support_conversations WHERE id=?",id);
    } else {
      jdbc.update("UPDATE support_conversations SET updated_at=NOW(6),status='OPEN' WHERE id=?",id);
      notifyLibraryTeam("New reply in library support", "A college member replied to an open support conversation. Open Library support to respond.");
    }
    return messages(jwt,id).getLast();
  }

  @PatchMapping("/conversations/{id}/status")
  public Map<String,Object> status(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@RequestParam String value) {
    if(!isStaff(jwt))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only Librarian/Admin staff can change ticket status.");
    String v=value==null?"":value.toUpperCase(Locale.ROOT);if(!Set.of("OPEN","IN_PROGRESS","CLOSED").contains(v))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Status must be OPEN, IN_PROGRESS, or CLOSED.");
    int count=jdbc.update("UPDATE support_conversations SET status=?,assigned_staff_id=COALESCE(assigned_staff_id,?),closed_at=IF(?='CLOSED',NOW(6),NULL),updated_at=NOW(6) WHERE id=?",v,userId(jwt),v,id);
    if(count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Conversation not found.");return Map.of("id",id,"status",v);
  }


  private void notifyLibraryTeam(String title, String body) {
    jdbc.update("INSERT INTO notifications(user_id,type,title,body,link,`read`,created_at) " +
      "SELECT DISTINCT u.id,'SUPPORT_MESSAGE',?,?, '/support',FALSE,NOW(6) FROM users u " +
      "JOIN user_roles ur ON ur.user_id=u.id JOIN roles r ON r.id=ur.role_id " +
      "WHERE r.name IN ('ADMIN','LIBRARIAN') AND u.enabled=TRUE AND u.approval_status='APPROVED'", title, body);
  }

  private Conversation getConversation(Jwt jwt,long id){assertAccess(jwt,id);return jdbc.queryForObject("SELECT c.id,c.requester_user_id,u.full_name requester_name,u.email requester_email,c.subject,c.status,c.updated_at,0 unread_count FROM support_conversations c JOIN users u ON u.id=c.requester_user_id WHERE c.id=?",(rs,i)->new Conversation(rs.getLong("id"),rs.getLong("requester_user_id"),rs.getString("requester_name"),rs.getString("requester_email"),rs.getString("subject"),rs.getString("status"),rs.getTimestamp("updated_at").toInstant().toString(),0),id);}
  private void assertAccess(Jwt jwt,long id){Integer c=jdbc.queryForObject("SELECT COUNT(*) FROM support_conversations WHERE id=? AND (requester_user_id=? OR ?=TRUE)",Integer.class,id,userId(jwt),isStaff(jwt));if(c==null||c==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Conversation not found.");}
  private static long userId(Jwt jwt){return Long.parseLong(jwt.getSubject());}
  private static boolean isStaff(Jwt jwt){List<String>a=jwt.getClaimAsStringList("authorities");return a!=null&&(a.contains("ROLE_ADMIN")||a.contains("ROLE_LIBRARIAN"));}
  private static String clean(String s){return s==null||s.isBlank()?null:s.trim();}
  // Kept only to make staff-list row mapping compatible with the record shape across JDK versions.
  private static Map<String,Object> rsMap(java.sql.ResultSet rs) throws java.sql.SQLException {Map<String,Object> m=new HashMap<>();m.put("id",rs.getLong("id"));m.put("requester_user_id",rs.getLong("requester_user_id"));m.put("requester_name",rs.getString("requester_name"));m.put("requester_email",rs.getString("requester_email"));m.put("subject",rs.getString("subject"));m.put("status",rs.getString("status"));m.put("updated_at",rs.getTimestamp("updated_at").toInstant().toString());m.put("unread_count",rs.getLong("unread_count"));return m;}
  private static Conversation conv(Map<String,Object> r){return new Conversation((Long)r.get("id"),(Long)r.get("requester_user_id"),(String)r.get("requester_name"),(String)r.get("requester_email"),(String)r.get("subject"),(String)r.get("status"),(String)r.get("updated_at"),(Long)r.get("unread_count"));}
}
