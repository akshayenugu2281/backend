package com.library360.member;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoanPolicyRepository extends JpaRepository<LoanPolicy, Long> {
    Optional<LoanPolicy> findByMemberType(MemberType memberType);

    default LoanPolicy getByMemberType(MemberType memberType) {
        return findByMemberType(memberType).orElseThrow();
    }
}
