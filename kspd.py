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

result = []

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

def LB1(cost, node):
  return cost + dist[node] 


# path ler edge list olarak tutuluyor
def LB2(new_path, threshold):
  
  lb2 = 0
  for path in result: 
    intersection = set(path).intersection(set(new_path))
    ilength = 0
    for v1, v2, data in intersection.items():
      ilength += data['weight']
    
    path_length = 0
    for v1, v2, data in path.items():
      path_length += data['weight']
    

    lb2 = max(ilength * (1+1/threshold) - path_length)

  return lb2


result = [((1,2, {'weight': 10}), (2,3, {'weight': 1}), (3, 4, {'weight': 10}))]

path = [(1,2, {'weight': 10}), (2,6, {'weight': 1}), (6, 7, {'weight': 1}), (7, 5, {'weight': 15}), (5, 4, {'weight': 1})]

LB2(path, 0.5)


# def Sim(path):
