package com.review.module.review.service.impl;

import com.review.module.parser.dto.CrossReference;
import com.review.module.parser.dto.ParsedClause;
import com.review.module.parser.dto.ParsedContract;
import com.review.module.review.entity.ClauseEdgeDO;
import com.review.module.review.entity.ClauseNodeDO;
import com.review.module.review.repository.ClauseEdgeRepository;
import com.review.module.review.repository.ClauseNodeRepository;
import com.review.module.review.service.ClauseGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClauseGraphServiceImpl implements ClauseGraphService {

    private final ClauseNodeRepository clauseNodeRepository;
    private final ClauseEdgeRepository clauseEdgeRepository;

    @Override
    @Transactional
    public void buildGraph(Long taskId, ParsedContract parsedContract) {
        if (parsedContract == null || parsedContract.getClauses() == null) {
            return;
        }

        List<ClauseNodeDO> nodes = new ArrayList<>();
        List<ClauseEdgeDO> edges = new ArrayList<>();

        // Flatten clauses recursively to build nodes
        flattenClauses(taskId, parsedContract.getClauses(), nodes);

        // Build edges from cross-references
        if (parsedContract.getCrossReferences() != null) {
            for (CrossReference ref : parsedContract.getCrossReferences()) {
                edges.add(ClauseEdgeDO.builder()
                        .taskId(taskId)
                        .sourceClause(ref.getSourceClause())
                        .targetClause(ref.getTargetClause())
                        .relationType("references")
                        .build());
            }
        }

        // Build edges from definitions
        if (parsedContract.getDefinitions() != null) {
            Set<String> clauseNumbers = nodes.stream()
                    .map(ClauseNodeDO::getClauseNumber)
                    .collect(Collectors.toSet());
            for (Map.Entry<String, String> entry : parsedContract.getDefinitions().entrySet()) {
                if (clauseNumbers.contains(entry.getValue())) {
                    edges.add(ClauseEdgeDO.builder()
                            .taskId(taskId)
                            .sourceClause(entry.getValue())
                            .targetClause(entry.getKey())
                            .relationType("defines")
                            .build());
                }
            }
        }

        // Build parent-child edges (depends)
        buildParentChildEdges(taskId, parsedContract.getClauses(), null, edges);

        clauseNodeRepository.saveAll(nodes);
        clauseEdgeRepository.saveAll(edges);

        log.info("Built clause graph for taskId={}: {} nodes, {} edges", taskId, nodes.size(), edges.size());
    }

    @Override
    public List<ClauseNodeDO> findRelatedClauses(Long taskId, String clauseNumber) {
        List<ClauseEdgeDO> allEdges = clauseEdgeRepository.findByTaskId(taskId);
        List<ClauseNodeDO> allNodes = clauseNodeRepository.findByTaskId(taskId);

        Map<String, ClauseNodeDO> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(ClauseNodeDO::getClauseNumber, n -> n, (a, b) -> a));

        // BFS traversal to find connected clauses
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(clauseNumber);
        visited.add(clauseNumber);

        Map<String, List<ClauseEdgeDO>> adjacency = new HashMap<>();
        for (ClauseEdgeDO edge : allEdges) {
            adjacency.computeIfAbsent(edge.getSourceClause(), k -> new ArrayList<>()).add(edge);
            adjacency.computeIfAbsent(edge.getTargetClause(), k -> new ArrayList<>()).add(edge);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<ClauseEdgeDO> neighbors = adjacency.getOrDefault(current, Collections.emptyList());
            for (ClauseEdgeDO edge : neighbors) {
                String other = edge.getSourceClause().equals(current) ? edge.getTargetClause() : edge.getSourceClause();
                if (!visited.contains(other)) {
                    visited.add(other);
                    queue.add(other);
                }
            }
        }

        visited.remove(clauseNumber);
        return visited.stream()
                .map(nodeMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClauseEdgeDO> findConflicts(Long taskId) {
        return clauseEdgeRepository.findByTaskIdAndRelationType(taskId, "conflicts");
    }

    private void flattenClauses(Long taskId, List<ParsedClause> clauses, List<ClauseNodeDO> nodes) {
        if (clauses == null) return;
        for (ParsedClause clause : clauses) {
            nodes.add(ClauseNodeDO.builder()
                    .taskId(taskId)
                    .clauseNumber(clause.getClauseNumber())
                    .clauseTitle(clause.getClauseTitle())
                    .clauseType(clause.getClauseType())
                    .charOffset(clause.getCharOffset())
                    .charLength(clause.getCharLength())
                    .build());
            if (clause.getSubClauses() != null) {
                flattenClauses(taskId, clause.getSubClauses(), nodes);
            }
        }
    }

    private void buildParentChildEdges(Long taskId, List<ParsedClause> clauses, String parentNumber, List<ClauseEdgeDO> edges) {
        if (clauses == null) return;
        for (ParsedClause clause : clauses) {
            if (parentNumber != null) {
                edges.add(ClauseEdgeDO.builder()
                        .taskId(taskId)
                        .sourceClause(parentNumber)
                        .targetClause(clause.getClauseNumber())
                        .relationType("depends")
                        .build());
            }
            if (clause.getSubClauses() != null) {
                buildParentChildEdges(taskId, clause.getSubClauses(), clause.getClauseNumber(), edges);
            }
        }
    }
}
