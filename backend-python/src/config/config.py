import os
from dotenv import load_dotenv

# Cargar variables de entorno desde .env
load_dotenv()

class Config:
    """Configuración centralizada"""

    # Configuración del servicio
    PORT = int(os.getenv('PYTHON_PORT', 5000))
    DEBUG = os.getenv('DEBUG', 'True') == 'True'

    # URL del backend Java
    JAVA_URL = os.getenv('JAVA_BACKEND_URL', 'http://localhost:8080')

    # Credenciales de Java
    JAVA_USERNAME = os.getenv('JAVA_USERNAME')
    JAVA_PASSWORD = os.getenv('JAVA_PASSWORD')

    @classmethod
    def validate(cls):
        """Validar que las credenciales existen"""
        if not cls.JAVA_USERNAME or not cls.JAVA_PASSWORD:
            raise ValueError(
                " Credenciales de Java no configuradas. "
                "Asegúrate de tener JAVA_USERNAME y JAVA_PASSWORD en .env"
            )

    @classmethod
    def get_endpoints(cls):
        """Obtener endpoints de Java"""
        base_url = cls.JAVA_URL
        return {
            'estadisticas': f'{base_url}/admin-menu/estadisticas-dashboard',
            'ventas_recientes': f'{base_url}/admin-menu/ventas-recientes',
            'productos': f'{base_url}/admin-menu/productos',
            'usuarios': f'{base_url}/admin-menu/usuarios',
        }

# Crear instancia global
config = Config()