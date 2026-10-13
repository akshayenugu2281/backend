package com.library360.profile;

import com.library360.auth.RefreshTokenRepository;
import com.library360.member.MemberRepository;
import com.library360.user.User;
import com.library360.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/v1/profile") @RequiredArgsConstructor
public class ProfileController {
 private final UserRepository users; private final MemberRepository members; private final PasswordEncoder encoder; private final RefreshTokenRepository refreshTokens;
 public record ProfileResponse(Long id,String email,String fullName,String firstName,String lastName,String facultyId,String employeeNumber,String department,String designation,String admissionNumber,String rollNumber,String branch,String academicYear,String className,String section,LocalDate dateOfBirth,String phoneNumber,boolean enabled,String approvalStatus,List<String> roles){}
 public record UpdateProfileRequest(@NotBlank @Size(max=120) String fullName,@Size(max=80) String firstName,@Size(max=80) String lastName,@Size(max=40) String facultyId,@Size(max=40) String employeeNumber,@Size(max=100) String department,@Size(max=100) String designation,@Size(max=40) String admissionNumber,@Size(max=40) String rollNumber,@Size(max=80) String branch,@Size(max=30) String academicYear,@Size(max=50) String className,@Size(max=20) String section,LocalDate dateOfBirth,@Size(max=30) String phoneNumber){}
 public record ChangePasswordRequest(@NotBlank String currentPassword,@NotBlank @Size(min=6,max=72) String newPassword){}
 @GetMapping("/me") @Transactional(readOnly=true) public ProfileResponse me(@AuthenticationPrincipal Jwt jwt){return response(find(jwt));}
 @PutMapping("/me") @Transactional public ProfileResponse update(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody UpdateProfileRequest r){
  User u=find(jwt);if(r.dateOfBirth()!=null&&(r.dateOfBirth().isAfter(LocalDate.now().minusYears(16))||r.dateOfBirth().isBefore(LocalDate.now().minusYears(100))))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter a valid date of birth.");
  String admission=clean(r.admissionNumber()),roll=clean(r.rollNumber()),faculty=clean(r.facultyId()),employee=clean(r.employeeNumber());
  if(admission!=null&&users.existsByAdmissionNumberIgnoreCaseAndIdNot(admission,u.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Admission number is already in use.");
  if(roll!=null&&users.existsByRollNumberIgnoreCaseAndIdNot(roll,u.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Roll number is already in use.");
  if(faculty!=null&&users.existsByFacultyIdIgnoreCaseAndIdNot(faculty,u.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Faculty ID is already in use.");
  if(employee!=null&&users.existsByEmployeeNumberIgnoreCaseAndIdNot(employee,u.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Employee number is already in use.");
  String first=clean(r.firstName()),last=clean(r.lastName());
  if(first!=null||last!=null){if(first==null||last==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Both first name and last name are required when updating name fields.");u.setFirstName(first);u.setLastName(last);u.setFullName((first+" "+last).trim());}else u.setFullName(r.fullName().trim());
  u.setAdmissionNumber(admission);u.setRollNumber(roll);u.setFacultyId(faculty);u.setEmployeeNumber(employee);u.setBranch(clean(r.branch()));u.setAcademicYear(clean(r.academicYear()));u.setClassName(clean(r.className()));u.setSection(clean(r.section()));u.setDateOfBirth(r.dateOfBirth());u.setPhoneNumber(clean(r.phoneNumber()));u.setDepartment(clean(r.department()));u.setDesignation(clean(r.designation()));u.setProfileUpdatedAt(Instant.now());
  User saved=users.save(u);members.findByUserId(saved.getId()).ifPresent(m->{m.setPhone(saved.getPhoneNumber());m.setDepartment(saved.getRoles().stream().anyMatch(role->role.getName().equals("FACULTY"))?saved.getDepartment():saved.getBranch());members.save(m);});return response(saved);
 }
 @PostMapping("/change-password") @Transactional public Map<String,String> password(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ChangePasswordRequest r){User u=find(jwt);if(!encoder.matches(r.currentPassword(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Current password is incorrect.");if(encoder.matches(r.newPassword(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a password different from your current password.");if(!r.newPassword().matches("(?=.*[A-Za-z])(?=.*[0-9]).{6,72}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use at least 6 characters with both letters and numbers.");u.setPasswordHash(encoder.encode(r.newPassword()));users.save(u);refreshTokens.revokeAllForUser(u.getId());return Map.of("message","Password updated. Other refresh sessions have been revoked; sign in again on your other devices.");}
 private User find(Jwt jwt){return users.findByIdWithRoles(Long.valueOf(jwt.getSubject())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Account not found."));}
 private ProfileResponse response(User u){return new ProfileResponse(u.getId(),u.getEmail(),u.getFullName(),u.getFirstName(),u.getLastName(),u.getFacultyId(),u.getEmployeeNumber(),u.getDepartment(),u.getDesignation(),u.getAdmissionNumber(),u.getRollNumber(),u.getBranch(),u.getAcademicYear(),u.getClassName(),u.getSection(),u.getDateOfBirth(),u.getPhoneNumber(),u.isEnabled(),u.getApprovalStatus(),u.getRoles().stream().map(r->r.getName()).sorted().toList());}
 private static String clean(String s){return s==null||s.isBlank()?null:s.trim();}
}
