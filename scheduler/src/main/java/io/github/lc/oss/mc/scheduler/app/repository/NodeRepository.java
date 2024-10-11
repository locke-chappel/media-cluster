package io.github.lc.oss.mc.scheduler.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.Node;
import io.github.lc.oss.mc.scheduler.app.model.NodeTypes;

@Repository
public interface NodeRepository extends JpaRepository<Node, String> {
    Node findByNameIgnoreCase(String name);

    List<Node> findByTypeIn(NodeTypes... types);

    List<Node> findByTypeAndStatusIn(NodeTypes type, Status... statuses);

    List<Node> findByClusterNameIgnoreCaseAndStatusIn(String clusterName, Status... statues);

    Node findFirstByClusterNameIgnoreCaseAndAllowScanAndStatusIn(String cluserName, boolean allowScan,
            Status... stautes);

    @Query("SELECT DISTINCT n.clusterName FROM Node n WHERE n.clusterName IS NOT NULL")
    List<String> findAllDistinctClusterNames();

    List<Node> findByClusterNameIgnoreCase(String clusterName);
}
