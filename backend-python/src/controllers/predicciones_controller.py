# src/controllers/predicciones_controller.py
from flask import Blueprint, jsonify, request
from src.services.prediction_service import PredictionService

predicciones_bp = Blueprint('predicciones', __name__, url_prefix='/api/predicciones')

# Instancia global del servicio
prediction_service = PredictionService()
modelo_entrenado = False


try:
    print(" Intentando entrenar modelo automáticamente...")
    resultado = prediction_service.entrenar()
    if resultado.get('success'):
        modelo_entrenado = True
        print(f" Modelo entrenado automáticamente. Precisión: {resultado.get('precision')}%")
        print(f" Datos de entrenamiento: {resultado.get('datos_entrenamiento')}")
    else:
        print(f" No se pudo entrenar modelo: {resultado.get('message')}")
        print(" Las predicciones usarán el método simple (promedio)")
except Exception as e:
    print(f" Error entrenando modelo al iniciar: {e}")
    print(" Las predicciones usarán el método simple (promedio)")

@predicciones_bp.route('/entrenar', methods=['POST'])
def entrenar_modelo():
    """Endpoint para entrenar el modelo con datos históricos"""
    global prediction_service, modelo_entrenado
    print(" Entrenando modelo manualmente...")
    resultado = prediction_service.entrenar()
    if resultado.get('success'):
        modelo_entrenado = True
        print(f" Modelo entrenado exitosamente. Precisión: {resultado.get('precision')}%")
    else:
        print(f" Error entrenando modelo: {resultado.get('message')}")
    return jsonify(resultado)

@predicciones_bp.route('/ventas', methods=['GET'])
def predecir_ventas():
    """Endpoint para predecir ventas futuras"""
    global modelo_entrenado

    print(" Recibiendo solicitud de predicción...")


    if not modelo_entrenado:
        print(" Modelo no entrenado, intentando entrenar automáticamente...")
        resultado = prediction_service.entrenar()
        if resultado.get('success'):
            modelo_entrenado = True
            print(f" Modelo entrenado automáticamente. Precisión: {resultado.get('precision')}%")
        else:
            print(f" No se pudo entrenar: {resultado.get('message')}")
            print(" Usando método de predicción simple...")

    # Obtener días a predecir
    dias = request.args.get('dias', default=7, type=int)
    dias = min(max(dias, 1), 30)  # Limitar entre 1 y 30 días

    print(f" Prediciendo {dias} días...")

    # Obtener predicciones
    resultado = prediction_service.predecir(dias)

    # Verificar si las predicciones fueron exitosas
    if not resultado.get('success'):
        # Si falla, intentar con método simple
        print(" Falló la predicción ML, usando método simple...")
        resultado = prediction_service.predecir_simple(dias)

    return jsonify(resultado)

@predicciones_bp.route('/tendencias', methods=['GET'])
def analizar_tendencias():
    """Endpoint para analizar tendencias de ventas"""
    print(" Analizando tendencias...")
    resultado = prediction_service.analizar_tendencias()
    return jsonify(resultado)

@predicciones_bp.route('/status', methods=['GET'])
def status_modelo():
    """Endpoint para verificar el estado del modelo"""
    return jsonify({
        'modelo_entrenado': modelo_entrenado,
        'metodo': 'ML' if modelo_entrenado else 'Simple (promedio)',
        'mensaje': 'Modelo entrenado y listo' if modelo_entrenado else 'Modelo no entrenado, usando predicciones simples'
    })