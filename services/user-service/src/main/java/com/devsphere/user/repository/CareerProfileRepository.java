package com.devsphere.user.repository;

import com.devsphere.user.entity.CareerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CareerProfileRepository extends JpaRepository<CareerProfile, Long> {

    Optional<CareerProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);
}
