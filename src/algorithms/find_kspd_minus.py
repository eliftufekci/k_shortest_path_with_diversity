from typing import Dict, List, Optional
import heapq
import networkx as nx

from .base import BasePathFindingAlgorithm
from ..core.data_structures import Path, GraphState
from ..core.graph_utils import (
    reverse, 
    dijkstra
)
from ..core.prefix_map import PrefixMap


class FindKSPD_Minus(BasePathFindingAlgorithm):
    def __init__(self, graph: nx.DiGraph, threshold: float = 0.5):
        super().__init__(graph, threshold)
        self.prefix_map = PrefixMap()
        self.global_LQ_ids = set()
        self.number_of_paths_explored = 0

    def find_paths(
        self,
        src: int,
        dest: int,
        k: int
    ) -> List[Path]:

        self.validate_parameters(src, dest, k)

        graph_reverse = reverse(self.graph)

        graph_state = GraphState(graph_reverse, dest)

        result_set: List[Path] = []

        shortest_path = dijkstra(self.graph, src, dest)
        if shortest_path is None:
            return result_set

        result_set.append(shortest_path)

        global_pq: List = [] # Global Priority Queue
        lq: Dict[int, List[Path]] = {} # Local Priority Queues per node
        covered_vertices: Dict = {} # Covered vertices for class-based paths

        self._generate_initial_paths(
            shortest_path, src, dest, global_pq, lq, covered_vertices, graph_state
        )

        while len(result_set) < k and global_pq:
            new_path = self._find_next_path(graph_state, global_pq, lq, result_set, dest, covered_vertices)

            if new_path and new_path.similarity(threshold=self.threshold, result_set=result_set):
                result_set.append(new_path)

        return result_set


    def _generate_initial_paths(
            self,
            shortest_path: Path,
            src: int,
            dest: int,
            global_pq: List,
            lq: Dict[int, List[Path]],
            covered_vertices: Dict,
            graph_state: GraphState
        ) -> None:

        for vertex in shortest_path.route[:-1]:
            for neighbor in self.graph[vertex]:
                path = Path()
                path.route = shortest_path.route[:shortest_path.route.index(vertex)+1]

                should_add = False

                if neighbor not in path.route:
                    if neighbor in shortest_path.route:
                        next_idx = shortest_path.route.index(vertex) + 1
                        if next_idx < len(shortest_path.route) and shortest_path.route[next_idx] != neighbor:
                            path.route.append(neighbor)
                            should_add = True

                    else:
                        path.route.append(neighbor)
                        should_add = True

                if should_add:
                    for i in range(len(path.route)-1):
                        u, v = path.route[i], path.route[i+1]
                        path.edges[(u,v)] = self.graph[u][v]['weight']
                        path.length += self.graph[u][v]['weight']
                    path.cls = (1, vertex)
                    path.lb = path.LB1(graph_state)

                    tail = path.tail()
                    if tail not in lq:
                        lq[tail] = []
                    heapq.heappush(lq[tail], path)
                    self.prefix_map.insert(path)

                    if lq[tail] and id(lq[tail]) not in self.global_LQ_ids:
                        heapq.heappush(global_pq, ((not lq[tail][0].is_active, lq[tail][0].lb), id(lq[tail]), lq[tail]))
                        self.global_LQ_ids.add(id(lq[tail]))


    def _extend_path(
            self,
            path: Path,
            graph_state: GraphState,
            lq: Dict[int, List[Path]],
            global_pq: List,
            covered_vertices: Dict
        ) -> bool:

        tail = path.tail()

        if tail in lq:
            for p in lq[tail]:
                if p.cls == path.cls and p.length >= path.length and p.is_active:
                    p.is_active = False

        for neighbor in self.graph[tail]:
            if neighbor not in path.route and neighbor != graph_state.parent.get(tail):
                new_path = path.copy()
                new_path.route.append(neighbor)

                edge_weight = self.graph[tail][neighbor]['weight']
                new_path.edges[(tail, neighbor)] = edge_weight
                new_path.length += edge_weight
                new_path.lb = new_path.LB1(graph_state)

                class_key = path.cls
                if class_key not in covered_vertices:
                    covered_vertices[class_key] = set()

                if neighbor in covered_vertices[class_key]:
                    new_path.is_active = False
                else:
                    covered_vertices[class_key].add(neighbor)

                if neighbor not in lq:
                    lq[neighbor] = []
                heapq.heappush(lq[neighbor], new_path)
                self.prefix_map.insert(new_path)


        parent = graph_state.parent.get(tail)
        if parent is None:
            return False

        if parent in path.route:
            self.prefix_map.remove(path)
            return False

        path.route.append(parent)
        edge_weight = self.graph[tail][parent]['weight']
        path.edges[(tail, parent)] = edge_weight
        path.length += edge_weight

        return True

    def _adjust_path(
        self,
        path: Path,
        lq: Dict[int, List[Path]],
        global_pq: List,
        result_set: List[Path],
        dest: int,
        prefix_map: PrefixMap
        ) -> None:
        if path.cls is None:
            return

        _, deviation_vertex = path.cls

        updated = False
        for vertex in path.route:
            if vertex in lq:
                for p in lq[vertex]:
                    if not p.is_active:
                        if p.cls == path.cls:
                            p.is_active = True
                            updated = True

        if path.tail() == dest:
            path_id = len(result_set) + 1

            for i, vertex in enumerate(path.route):
                prefix = path.route[:i+1]

                if len(prefix)>1:
                    paths_with_prefix = self.prefix_map.findPathListWithPrefix(prefix)

                    if paths_with_prefix:
                        for p in paths_with_prefix:
                            if len(p.route) > len(prefix):
                                p.cls = (path_id, vertex)

        if updated:
            for i in range(len(global_pq)):
                temp_queue = global_pq[i][2]

                if temp_queue:
                    first_active = next((p for p in temp_queue if p.is_active), None)
                    if first_active:
                        new_key = (not first_active.is_active, first_active.lb)
                        global_pq[i] = (new_key, global_pq[i][1], temp_queue)

            heapq.heapify(global_pq)


    def _find_next_path(
            self,
            graph_state: GraphState,
            global_pq: List,
            lq: Dict[int, List[Path]],
            result_set: List[Path],
            dest: int,
            covered_vertices: Dict
        ) -> Optional[Path]:
        self.number_of_paths_explored += 1

        while global_pq:
            _, _, current_LQ = heapq.heappop(global_pq)

            if not current_LQ:
                continue

            inactive_paths = []
            current_path = None

            while current_LQ:
                candidate = heapq.heappop(current_LQ)

                if candidate.is_active:
                    current_path = candidate
                    break
                else:
                    inactive_paths.append(candidate)

            for p in inactive_paths:
                heapq.heappush(current_LQ, p)

            if current_path is None:
                if current_LQ:
                    heapq.heappush(global_pq, ((not current_LQ[0].is_active, current_LQ[0].lb), id(current_LQ), current_LQ))
                continue

            if current_LQ:
                heapq.heappush(global_pq, ((not current_LQ[0].is_active, current_LQ[0].lb), id(current_LQ), current_LQ))

            while current_path.tail() != dest:
                if not self._extend_path(path=current_path, graph_state=graph_state, lq=lq, global_pq=global_pq, covered_vertices=covered_vertices):
                    break

            if current_path.tail() == dest:
                if current_path.cls in covered_vertices:
                    covered_vertices[current_path.cls].clear()

                self.prefix_map.remove(current_path)

                self._adjust_path(path=current_path, global_pq=global_pq, lq=lq, result_set=result_set, dest=dest, prefix_map=self.prefix_map)

                return current_path

        return None