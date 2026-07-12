from flask import Blueprint, jsonify, request
from src.services.dashboard_service import DashboardService

dashboard_bp = Blueprint('dashboard', __name__, url_prefix='/api/dashboard')

@dashboard_bp.route('/estadisticas', methods=['GET'])
def obtener_estadisticas_dashboard():
    """Endpoint para estadísticas del dashboard"""
    service = DashboardService()
    resultado = service.obtener_estadisticas_dashboard()
    return jsonify(resultado)

@dashboard_bp.route('/ventas-recientes', methods=['GET'])
def obtener_ventas_recientes():
    """Endpoint para ventas recientes"""
    service = DashboardService()
    limit = request.args.get('limit', 10, type=int)
    resultado = service.obtener_ventas_recientes(limit)
    return jsonify({
        'success': True,
        'data': resultado,
        'total': len(resultado) if isinstance(resultado, list) else 0
    })

@dashboard_bp.route('/estadisticas-ventas', methods=['GET'])
def obtener_estadisticas_ventas():
    """Endpoint para estadísticas de ventas por día"""
    service = DashboardService()
    resultado = service.obtener_estadisticas_ventas()
    return jsonify(resultado)