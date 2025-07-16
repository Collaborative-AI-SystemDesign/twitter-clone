package com.example.demo.domain.test_cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.example.demo.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cassandra/cluster")
public class CassandraClusterController {

    private final CqlSession cqlSession;

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getClusterInfo() {
        Map<String, Object> clusterInfo = new HashMap<>();
        
        Metadata metadata = cqlSession.getMetadata();
        
        clusterInfo.put("clusterName", metadata.getClusterName().orElse("Unknown"));
        clusterInfo.put("keyspaces", metadata.getKeyspaces().keySet());
        clusterInfo.put("nodeCount", metadata.getNodes().size());
        
        return ApiResponse.success(clusterInfo);
    }
    
    @GetMapping("/nodes")
    public ApiResponse<Map<String, Object>> getNodesInfo() {
        Map<String, Object> nodesInfo = new HashMap<>();
        
        Metadata metadata = cqlSession.getMetadata();
        
        nodesInfo.put("nodes", metadata.getNodes().values().stream()
                .collect(Collectors.toMap(
                        node -> node.getEndPoint().toString(),
                        node -> Map.of(
                                "state", node.getState().toString(),
                                "datacenter", node.getDatacenter(),
                                "rack", node.getRack(),
                                "hostId", node.getHostId() != null ? node.getHostId().toString() : "Unknown"
                        )
                )));
        
        return ApiResponse.success(nodesInfo);
    }
    
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> getClusterHealth() {
        Map<String, Object> health = new HashMap<>();
        
        Metadata metadata = cqlSession.getMetadata();
        
        long totalNodes = metadata.getNodes().size();
        
        health.put("totalNodes", totalNodes);
        health.put("nodesAvailable", totalNodes > 0);
        health.put("sessionConnected", cqlSession != null && !cqlSession.isClosed());
        
        return ApiResponse.success(health);
    }
} 