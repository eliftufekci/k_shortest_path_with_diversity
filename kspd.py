import networkx as nx
import heapq
G = nx.DiGraph()
edges = [(1, 2, 10),
         (2, 3, 1),
         (3, 4, 10),
         (1, 9, 20),
         (9, 2, 1),
         (2, 6, 1),
         (6, 8, 1),
         (8, 9, 1),
         (2, 8, 3),
         (8, 5, 15),
         (3, 5, 18),
         (5, 4, 1)]

# A 1
# B 2
# C 3
# D 4
# E 5
# F 6
# G 7
# H 8
# I 9

G.add_weighted_edges_from(edges)
nx.draw(G, with_labels=True)
# partial dijkstra
def dijkstra(graph, src, dest):

  if src ==  dest:
    return 0

  heap = [(0, src)]
  shortest_path = {node: float('inf') for node in graph}
  shortest_path[src] = 0

  while heap:
    cost, node = heapq.heappop(heap)
    if node == dest:
      # print(shortest_path)
      return shortest_path[dest]

    if cost > shortest_path[node]:
      continue

    for neighbor, data in graph[node].items():
      new_cost = cost + data['weight']

      if new_cost < shortest_path[neighbor]:
        shortest_path[neighbor] = new_cost
        heapq.heappush(heap, (new_cost, neighbor))

  return float('inf')
 
def reverse(graph):
  Gr = nx.DiGraph()
  # for node in graph.nodes():
  #   Gr.add_node(node)

  Gr.add_edges_from((v,u,d) for u,v,d in graph.edges(data=True))
  return Gr
GR = reverse(G)
nx.draw(GR, with_labels=True)
dist = {}
isSettled = {}
PQ = []
parent = {}

for node in GR.nodes():
  dist[node] = float('inf')
  isSettled[node] = False
  parent[node] = None

dest = 4 # D vertexi destination noktamız
heapq.heappush(PQ, (0, dest))
dist[dest] = 0

def ConstructPartialSPT(graph, v):
  global dist, isSettled, PQ, parent

  if isSettled[v]:
    return dist[v]

  while PQ:
    cost, node = heapq.heappop(PQ)

    if cost > dist[node]:
      continue

    if not isSettled[node]:
      isSettled[node] = True

      if node == v:
        return dist[v]      
      
      for neighbor, data in graph[node].items():

        if not isSettled[neighbor]:        
          new_cost = cost + data['weight']

          if new_cost < dist[neighbor]:
            dist[neighbor] = new_cost
            parent[neighbor] = node
            heapq.heappush(PQ, (new_cost, neighbor))
        
  return float('inf')

ConstructPartialSPT(GR, 5)
print(dist)
print(isSettled)
print(parent)

ConstructPartialSPT(GR, 2)
print(dist)
print(isSettled)
print(parent)

def LB1(length, node):
  return length + dist[node] 

result = [{(1,2): 10,
           (2,3): 1,
           (3, 4): 10}]

# path ler edge list olarak tutuluyor
def LB2(new_path, threshold):
  global result
  lb2 = 0
  for old_path in result: 

    common_edges = set(old_path.keys()).intersection(set(new_path.keys()))

    intersection_length = sum(old_path[e] for e in common_edges)
    # print(intersection_length)
    old_path_length = sum(e for e in old_path.values())
    # print(old_path_length)

    current_lb2 = intersection_length * (1+1/threshold) - old_path_length
    lb2 = max(lb2, current_lb2)

  #   if old_path in new_path:
  #     s += result[old_path]

  # print(s)
  # ss = sum(result.values())
  return lb2


path = {(1,2): 10,
        (2,6): 1,
        (6, 7): 1,
        (7, 5): 15,
        (5, 4): 1}

LB2(path, 0.5)
# def Sim(path): 