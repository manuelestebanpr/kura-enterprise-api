package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByCedulaAndDeletedAtIsNull(String cedula);
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    boolean existsByCedula(String cedula);
    boolean existsByEmail(String email);
}
