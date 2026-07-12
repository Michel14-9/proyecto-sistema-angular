import requests
from src.config.config import config

class JavaRepository:
    """Repositorio para consumir datos de Java (endpoints públicos)"""

    def __init__(self):
        self.base_url = config.JAVA_URL

    def get_productos(self):
        """Obtener productos desde Java"""
        try:
            response = requests.get(
                f'{self.base_url}/admin/data/productos',
                timeout=10
            )
            if response.status_code == 200:
                return response.json()
            else:
                print(f" Error get_productos: {response.status_code}")
                return []
        except Exception as e:
            print(f" Error al obtener productos: {e}")
            return []

    def get_usuarios(self):
        """Obtener usuarios desde Java"""
        try:
            response = requests.get(
                f'{self.base_url}/admin/data/usuarios',
                timeout=10
            )
            if response.status_code == 200:
                return response.json()
            else:
                print(f" Error get_usuarios: {response.status_code}")
                return []
        except Exception as e:
            print(f" Error al obtener usuarios: {e}")
            return []

    def get_pedidos(self):
        """Obtener todos los pedidos desde Java"""
        try:
            response = requests.get(
                f'{self.base_url}/admin/data/pedidos',
                timeout=10
            )
            if response.status_code == 200:
                return response.json()
            else:
                print(f" Error get_pedidos: {response.status_code}")
                return []
        except Exception as e:
            print(f" Error al obtener pedidos: {e}")
            return []

    def get_pedidos_entregados(self):
        """Obtener pedidos entregados desde Java"""
        try:
            response = requests.get(
                f'{self.base_url}/admin/data/pedidos/entregados',
                timeout=10
            )
            if response.status_code == 200:
                return response.json()
            else:
                print(f" Error get_pedidos_entregados: {response.status_code}")
                return []
        except Exception as e:
            print(f" Error al obtener pedidos entregados: {e}")
            return []

    def get_pedidos_pagados(self):
        """Obtener pedidos pagados desde Java"""
        try:
            response = requests.get(
                f'{self.base_url}/admin/data/pedidos/pagados',
                timeout=10
            )
            if response.status_code == 200:
                return response.json()
            else:
                print(f" Error get_pedidos_pagados: {response.status_code}")
                return []
        except Exception as e:
            print(f" Error al obtener pedidos pagados: {e}")
            return []