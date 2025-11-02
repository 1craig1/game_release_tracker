package com.gamereleasetracker.repository;

import com.gamereleasetracker.model.Role;
import com.gamereleasetracker.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    /* An Optional is a container object that may or may not hold a non-null value.
     It's used to explicitly handle cases where a value could be absent, helping to
     prevent NullPointerExceptions without relying on null checks. */
    Optional<Role> findByName(RoleType name);
}
