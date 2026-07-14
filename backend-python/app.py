from flask import Flask, jsonify
from flask_cors import CORS
from src.config.config import config
from src.controllers.dashboard_controller import dashboard_bp
from src.controllers.reportes_controller import reportes_bp
from src.controllers.predicciones_controller import predicciones_bp

app = Flask(__name__)

# ============ CONFIGURACIÓN CORS CORREGIDA ============
CORS(app,
     origins=[
         "http://localhost:4200",
         "http://localhost",
         "http://localhost:80",
         "http://127.0.0.1",
         "http://127.0.0.1:4200",
         "http://localhost:5000"
     ],
     supports_credentials=True,
     methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
     allow_headers=["Content-Type", "Authorization", "X-Requested-With", "Accept"],
     expose_headers=["Content-Type", "Authorization"]
     )

# ============ REGISTRAR BLUEPRINTS ============
app.register_blueprint(dashboard_bp)
app.register_blueprint(reportes_bp)
app.register_blueprint(predicciones_bp)

# ============ HEALTH CHECK ============
@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({
        'status': 'ok',
        'service': 'backend-python-dashboard',
        'version': '1.0.0',
        'message': '¡Backend Python funcionando! ',
        'java_connected': config.JAVA_URL
    })

# ============ TEST CONEXIÓN CON JAVA ============
@app.route('/api/test/java', methods=['GET'])
def test_java_connection():
    import requests
    try:
        response = requests.get(
            f'{config.JAVA_URL}/admin/data/productos',
            timeout=10
        )
        if response.status_code == 200:
            productos = response.json()
            return jsonify({
                'success': True,
                'status': response.status_code,
                'message': 'Conexión con Java exitosa',
                'productos_encontrados': len(productos)
            })
        else:
            return jsonify({
                'success': False,
                'status': response.status_code,
                'message': f'Error al conectar con Java: {response.status_code}'
            }), response.status_code
    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

# ============ INICIAR SERVIDOR ============
if __name__ == '__main__':
    print("=" * 60)
    print(" BACKEND PYTHON - DASHBOARD Y REPORTES")
    print("=" * 60)
    print(f" Puerto: {config.PORT}")
    print(f" Java: {config.JAVA_URL}")
    print("")
    print(" ENDPOINTS DISPONIBLES:")
    print(f"   Health:      http://localhost:{config.PORT}/api/health")
    print(f"   Test Java:   http://localhost:{config.PORT}/api/test/java")
    print(f"  Dashboard:   http://localhost:{config.PORT}/api/dashboard/estadisticas")
    print(f"   Ventas:      http://localhost:{config.PORT}/api/dashboard/ventas-recientes")
    print(f"   Estadísticas: http://localhost:{config.PORT}/api/dashboard/estadisticas-ventas")
    print(f"   Reportes:    http://localhost:{config.PORT}/api/reportes/ventas")
    print(f"   Completo:    http://localhost:{config.PORT}/api/reportes/completo")
    print(f"   Entrenar:    http://localhost:{config.PORT}/api/predicciones/entrenar (POST)")
    print(f"   Predicciones: http://localhost:{config.PORT}/api/predicciones/ventas?dias=7")
    print(f"   Tendencias:  http://localhost:{config.PORT}/api/predicciones/tendencias")
    print("=" * 60)
    print(" Servidor listo para recibir peticiones")
    print("")

    app.run(
        host='0.0.0.0',
        port=config.PORT,
        debug=config.DEBUG
    )