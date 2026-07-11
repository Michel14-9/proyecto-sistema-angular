# src/controllers/reportes_controller.py
from flask import Blueprint, jsonify, request
from src.services.reportes_service import ReportesService

reportes_bp = Blueprint('reportes', __name__, url_prefix='/api/reportes')

@reportes_bp.route('/ventas', methods=['GET'])
def obtener_reporte_ventas():
    """Obtener reporte de ventas"""
    service = ReportesService()
    fecha_inicio = request.args.get('fechaInicio')
    fecha_fin = request.args.get('fechaFin')
    tipo = request.args.get('tipo', 'ventas')
    resultado = service.obtener_reporte_ventas(fecha_inicio, fecha_fin, tipo)
    return jsonify(resultado)

@reportes_bp.route('/completo', methods=['GET'])
def obtener_reporte_completo():
    """Obtener reporte completo con estadísticas avanzadas"""
    service = ReportesService()
    #  Recibir fechas del frontend
    fecha_inicio = request.args.get('fechaInicio')
    fecha_fin = request.args.get('fechaFin')
    resultado = service.obtener_reporte_completo(fecha_inicio, fecha_fin)
    return jsonify(resultado)