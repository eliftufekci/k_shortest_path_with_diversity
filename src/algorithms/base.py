from abc import ABC, abstractmethod
from typing import List, Optional
import networkx as nx
from ..core.data_structures import Path


class BasePathFindingAlgorithm(ABC):    
    def __init__(self, graph: nx.DiGraph, threshold: float = 0.5):
        self.graph = graph
        self.threshold = threshold
    
    @abstractmethod
    def find_paths(
        self, 
        src: int, 
        dest: int, 
        k: int
    ) -> List[Path]:
        """
        K en uygun yolu bul.
        
        Args:
            src: Başlangıç vertex
            dest: Hedef vertex
            k: Kaç yol bulunacak
        
        Returns:
            Path listesi
        """
        pass
    
    def validate_parameters(self, src: int, dest: int, k: int) -> None:
        """Parametreleri kontrol et."""
        if src not in self.graph:
            raise ValueError(f"Source vertex {src} grafte yok")
        if dest not in self.graph:
            raise ValueError(f"Destination vertex {dest} grafte yok")
        if k < 1:
            raise ValueError("k en az 1 olmalı")
        if not (0 < self.threshold < 1):
            raise ValueError("Threshold 0 ile 1 arasında olmalı")