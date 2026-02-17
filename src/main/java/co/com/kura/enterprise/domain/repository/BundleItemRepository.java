package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.BundleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BundleItemRepository extends JpaRepository<BundleItem, UUID> {
    List<BundleItem> findByBundleIdOrderBySortOrder(UUID bundleId);
    void deleteByBundleId(UUID bundleId);
}
