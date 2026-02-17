package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.TestDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestDependencyRepository extends JpaRepository<TestDependency, UUID> {
    List<TestDependency> findByServiceId(UUID serviceId);
    List<TestDependency> findByItemCode(String itemCode);
}
