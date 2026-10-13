package com.library360.admin;

import com.library360.member.*;
import com.library360.user.*;
import com.library360.common.PageResponse;
import com.library360.auth.RefreshTokenRepository;
import com.library360.reservation.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.*;

@RestController @RequestMapping("/api/v1/admin/users") @RequiredArgsConstructor
public class AdminUserController {
 private final UserRepository users; private final RoleRepository roles; private final MemberRepository members; private final PasswordEncoder encoder; private final RefreshTokenRepository refreshTokens; private final ReservationService reservationService;
 public record CreateUserRequest(@Email @NotBlank String email,@NotBlank @Size(min=6,max=72) String temporaryPassword,@NotBlank @Size(max=120) String fullName,@NotBlank String role,String phoneNumber,String admissionNumber,String rollNumber,String branch,String academicYear,String className,String section,String facultyId,String employeeNumber,String department,String designation,LocalDate dateOfBirth){}
 public record UserRow(Long id,String email,String fullName,List<String> roles,boolean enabled,String approvalStatus,String phoneNumber,LocalDate dateOfBirth,String admissionNumber,String rollNumber,String branch,String academicYear,String className,String section,String facultyId,String employeeNumber,String department,String designation,String memberCode){}
 @GetMapping @Transactional(readOnly=true) public PageResponse<UserRow> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String q,@RequestParam(required=false) String role){String search=clean(q);String roleFilter=clean(role);if(roleFilter!=null)roleFilter=roleFilter.toUpperCase(Locale.ROOT);if(roleFilter!=null&&!Set.of("ADMIN","LIBRARIAN","FACULTY","STUDENT").contains(roleFilter))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid role filter.");var result=users.searchUsers(search,roleFilter,PageRequest.of(Math.max(0,page),Math.min(Math.max(size,1),100)));return PageResponse.from(result,this::row);}
 @PostMapping @Transactional public UserRow create(@Valid @RequestBody CreateUserRequest r){
  String email=r.email().trim().toLowerCase(Locale.ROOT);if(users.existsByEmail(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists.");
  String rn=roleName(r.role());if(!Set.of("STUDENT","FACULTY","LIBRARIAN").contains(rn))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Admin can create Student, Faculty or Librarian accounts. Admin accounts are bootstrapped securely outside this form.");
   if(!strongPassword(r.temporaryPassword()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Temporary password needs at least 6 characters with both letters and numbers.");
  User u=new User(email,encoder.encode(r.temporaryPassword()),r.fullName().trim());
  String[] parts=r.fullName().trim().split("\\s+",2);u.setFirstName(parts[0]);u.setLastName(parts.length>1?parts[1]:"");u.setApprovalStatus("APPROVED");
  u.setPhoneNumber(clean(r.phoneNumber()));u.setAdmissionNumber(clean(r.admissionNumber()));u.setRollNumber(clean(r.rollNumber()));u.setBranch(clean(r.branch()));u.setAcademicYear(clean(r.academicYear()));u.setClassName(clean(r.className()));u.setSection(clean(r.section()));u.setFacultyId(clean(r.facultyId()));u.setEmployeeNumber(clean(r.employeeNumber()));u.setDepartment(clean(r.department()));u.setDesignation(clean(r.designation()));u.setDateOfBirth(r.dateOfBirth());
  Role role=roles.findByName(rn).orElseThrow(()->new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Role seed missing: "+rn));u.getRoles().add(role);users.saveAndFlush(u);
  if(rn.equals("STUDENT")||rn.equals("FACULTY"))createMember(u,rn);
  return row(u);
 }
 @PatchMapping("/{id}/suspension") @Transactional public UserRow suspend(@PathVariable Long id,@RequestParam boolean suspended){User u=find(id);protectAdmin(u);if(!suspended&&!"APPROVED".equalsIgnoreCase(u.getApprovalStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Pending/rejected accounts must be approved through the verification queue first.");u.setEnabled(!suspended);users.save(u);if(suspended)refreshTokens.revokeAllForUser(id);members.findByUserId(id).ifPresent(m->{m.setStatus(suspended?MemberStatus.SUSPENDED:MemberStatus.ACTIVE);members.save(m);if(suspended)reservationService.cancelAllForMember(m.getId());});return row(u);}
 @PatchMapping("/{id}/role") @Transactional public UserRow role(@PathVariable Long id,@RequestParam String roleName){String target=roleName(roleName);if(!Set.of("STUDENT","FACULTY","LIBRARIAN").contains(target))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only Student, Faculty or Librarian roles can be assigned here.");User u=find(id);protectAdmin(u);if(!"APPROVED".equalsIgnoreCase(u.getApprovalStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Only approved accounts can have their role changed.");Role role=roles.findByName(target).orElseThrow(()->new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Role seed missing."));u.getRoles().clear();u.getRoles().add(role);users.save(u);refreshTokens.revokeAllForUser(id);Optional<Member> existing=members.findByUserId(id);if(Set.of("STUDENT","FACULTY").contains(target)){MemberType mt=MemberType.valueOf(target);if(existing.isPresent()){Member m=existing.get();m.setMemberType(mt);m.setStatus(u.isEnabled()?MemberStatus.ACTIVE:MemberStatus.SUSPENDED);m.setDepartment(target.equals("FACULTY")?u.getDepartment():u.getBranch());members.save(m);}else createMember(u,target);}else existing.ifPresent(m->{m.setStatus(MemberStatus.SUSPENDED);members.save(m);reservationService.cancelAllForMember(m.getId());});return row(u);}
 @PostMapping("/{id}/reset-password") @Transactional public Map<String,Object> resetPassword(@PathVariable Long id){
  User u=find(id);protectAdmin(u);
  byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);
  String temporary=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)+"Aa1!";
  u.setPasswordHash(encoder.encode(temporary));users.save(u);refreshTokens.revokeAllForUser(id);
  return Map.of("userId",id,"email",u.getEmail(),"temporaryPassword",temporary,"message","Temporary password generated once. Share it privately; the user must change it immediately.");
 }
 @DeleteMapping("/{id}") @Transactional public Map<String,Object> delete(@PathVariable Long id){User u=find(id);protectAdmin(u);u.setEnabled(false);if("APPROVED".equalsIgnoreCase(u.getApprovalStatus()))u.setApprovalStatus("REJECTED");users.save(u);refreshTokens.revokeAllForUser(id);members.findByUserId(id).ifPresent(m->{m.setStatus(MemberStatus.SUSPENDED);members.save(m);reservationService.cancelAllForMember(m.getId());});return Map.of("id",id,"deleted",true,"message","Account access disabled; circulation history retained for audit compliance.");}
 private User find(Long id){return users.findByIdWithRoles(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found."));}
 private void protectAdmin(User u){if(u.getRoles().stream().anyMatch(r->r.getName().equals("ADMIN")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Administrator accounts cannot be modified through this endpoint.");}
 private void createMember(User u,String type){Member m=new Member();m.setUser(u);m.setMemberCode((type.equals("STUDENT")?"STU-":"FAC-")+LocalDate.now().getYear()+"-"+u.getId());m.setMemberType(MemberType.valueOf(type));m.setStatus(MemberStatus.ACTIVE);m.setValidUntil(LocalDate.now().plusYears(type.equals("STUDENT")?4:10));m.setDepartment(type.equals("FACULTY")?u.getDepartment():u.getBranch());m.setPhone(u.getPhoneNumber());members.save(m);}
 private UserRow row(User u){String code=members.findByUserId(u.getId()).map(Member::getMemberCode).orElse(null);return new UserRow(u.getId(),u.getEmail(),u.getFullName(),u.getRoles().stream().map(Role::getName).sorted().toList(),u.isEnabled(),u.getApprovalStatus(),u.getPhoneNumber(),u.getDateOfBirth(),u.getAdmissionNumber(),u.getRollNumber(),u.getBranch(),u.getAcademicYear(),u.getClassName(),u.getSection(),u.getFacultyId(),u.getEmployeeNumber(),u.getDepartment(),u.getDesignation(),code);}
 private static String roleName(String value){return value==null?"":value.trim().toUpperCase(Locale.ROOT);}private static String clean(String s){return s==null||s.isBlank()?null:s.trim();}private static boolean strongPassword(String s){return s!=null&&s.matches("(?=.*[A-Za-z])(?=.*[0-9]).{6,72}");}
}
