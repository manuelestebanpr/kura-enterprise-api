package co.com.kura.enterprise.domain.repository;

import co.com.kura.enterprise.domain.entity.WarehouseInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, UUID> {
    List<WarehouseInventory> findByPosId(UUID posId);
    Optional<WarehouseInventory> findByPosIdAndItemCode(UUID posId, String itemCode);
}
