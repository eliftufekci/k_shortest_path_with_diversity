from typing import Tuple

class PrefixMap:
    def __init__(self):
        self.map = {}

    def insert(self, path):
        for i in range(len(path.route)):
            prefix = tuple(path.route[:i+1])

            if prefix not in self.map:
                self.map[prefix] = []

            self.map[prefix].append(path)

    def remove(self, path):
        for i in range(len(path.route)):
            prefix = tuple(path.route[:i+1])
            if prefix in self.map:
                try:
                    self.map[prefix].remove(path)
                except ValueError:
                    pass

    def findPathListWithPrefix(self, route):
        return self.map.get(tuple(route), [])