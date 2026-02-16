import heapq
import networkx as nx
from typing import Optional, Tuple, Dict, List
from .data_structures import Path, GraphState

def reverse(graph: nx.DiGraph) -> nx.DiGraph:
    Gr = nx.DiGraph()
    Gr.add_edges_from((v,u,d) for u,v,d in graph.edges(data=True))
    return Gr


def dijkstra(
    graph: nx.DiGraph, 
    src: int, 
    dest: int
    ) -> Optional[Path]:    
    
    if src == dest:
        path = Path()
        path.route = [src]
        return path

    heap = [(0, src, [])]
    visited = set()

    while heap:
        cost, node, path_list = heapq.heappop(heap)

        if node in visited:
            continue
        visited.add(node)

        if node == dest:
            shortest_path = Path()

            for i in range(len(path_list)):
                if i < len(path_list) - 1:
                    u, v = path_list[i], path_list[i+1]
                    shortest_path.edges[(u, v)] = graph[u][v]['weight']
                    shortest_path.length += graph[u][v]['weight']
                    shortest_path.route.append(u)
                else:
                    u, v = path_list[i], dest
                    shortest_path.edges[(u, v)] = graph[u][v]['weight']
                    shortest_path.length += graph[u][v]['weight']
                    shortest_path.route.append(u)
                    shortest_path.route.append(v)

            shortest_path.lb = shortest_path.length
            return shortest_path

        for neighbor, data in graph[node].items():
            if neighbor not in visited:
                new_cost = cost + data['weight']
                heapq.heappush(heap, (new_cost, neighbor, path_list + [node]))

    return None

def construct_partial_spt(graph_state: GraphState, v: int) -> float:
    if graph_state.isSettled[v]:
        return graph_state.distances[v]

    while graph_state.PQ:
        cost, node = heapq.heappop(graph_state.PQ)

        if cost > graph_state.distances[node]:
            continue

        if not graph_state.isSettled[node]:
            graph_state.isSettled[node] = True

            for neighbor, data in graph_state.graph_reverse[node].items():
                if not graph_state.isSettled[neighbor]:
                    new_cost = cost + data['weight']

                    if new_cost < graph_state.distances[neighbor]:
                        graph_state.distances[neighbor] = new_cost
                        graph_state.parent[neighbor] = node
                        heapq.heappush(graph_state.PQ, (new_cost, neighbor))

            if node == v:
                return graph_state.distances[v]

    return float('inf')


def _build_path(
    graph: nx.DiGraph, 
    path_list: List[int], 
    dest: int, 
    cost: float
) -> Path:
    shortest_path = Path()
    
    for i in range(len(path_list)):
        if i < len(path_list) - 1:
            u, v = path_list[i], path_list[i + 1]
        else:
            u, v = path_list[i], dest
        
        shortest_path.edges[(u, v)] = graph[u][v]['weight']
        shortest_path.length += graph[u][v]['weight']
        shortest_path.route.append(u)
    
    shortest_path.route.append(dest)
    shortest_path.lb = shortest_path.length
    return shortest_path
