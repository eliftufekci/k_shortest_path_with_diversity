import heapq
from typing import Dict, List, Optional, Tuple, Set
from dataclasses import dataclass, field

class GraphState:
    def __init__(self, graph_reverse, destination):
        self.graph_reverse = graph_reverse
        self.destination = destination
        self.distances = {}
        self.isSettled = {}
        self.parent = {}
        self.PQ = []

        for node in graph_reverse.nodes():
            self.distances[node] = float('inf')
            self.isSettled[node] = False
            self.parent[node] = None

        heapq.heappush(self.PQ, (0, destination))
        self.distances[destination] = 0

@dataclass
class Path:
    route: List[int] = field(default_factory=list)
    edges: Dict[Tuple[int, int], float] = field(default_factory=dict)
    length: float = 0.0
    lb: float = 0.0
    cls: Optional[Tuple] = None
    is_active: bool = True
    cached_intersections: Dict[int, float] = field(default_factory=dict)

    def __str__(self):
        return f"Route: {self.route}, Length: {self.length}, LB: {self.lb}, Class: {self.cls}, isActive: {self.isActive}"
    
    def __lt__(self, other: "Path") -> bool:
        return (not self.is_active, self.lb) < (not other.is_active, other.lb)
    
    def tail(self) -> Optional[int]:
        return self.route[-1] if self.route else None
    
    def head(self) -> Optional[int]:
        return self.route[0] if self.route else None
    
    def copy(self) -> "Path":
        """Deep Copy"""
        return Path(
            route=self.route.copy(),
            edges=self.edges.copy(),
            length=self.length,
            lb=self.lb,
            cls=self.cls,
            is_active=self.is_active,
            cached_intersections=self.cached_intersections.copy()
        )

    def LB1(self, graph_state):
        tail = self.tail()
        if tail is None:
            return 0

        if not graph_state.isSettled[tail]:
            ConstructPartialSPT(graph_state=graph_state, v=tail)

        return self.length + graph_state.distances[tail]

    def LB2(self, threshold, result_set):
        if not result_set:
            return 0

        lb2 = 0
        for old_path in result_set:
            if id(old_path) in self.cached_intersections:
                intersection_length = self.cached_intersections[id(old_path)] 
            
            else:
                common_edges = set(old_path.edges.keys()).intersection(set(self.edges.keys()))
                intersection_length = sum(old_path.edges[e] for e in common_edges)
                self.cached_intersections[id(old_path)] = intersection_length

            current_lb2 = intersection_length * (1 + 1/threshold) - old_path.length
            lb2 = max(lb2, current_lb2)

        return lb2


    def similarity(self, threshold, result_set):
        for old_path in result_set:
            common_edges = set(old_path.edges.keys()).intersection(set(self.edges.keys()))
            intersection_length = sum(old_path.edges[e] for e in common_edges)
            union_length = self.length + old_path.length - intersection_length

            if union_length > 0:
                similarity = intersection_length / union_length
                if similarity > threshold:
                    return False
        return True