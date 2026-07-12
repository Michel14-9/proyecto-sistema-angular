# src/services/prediction_service.py
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from sklearn.linear_model import LinearRegression
from sklearn.preprocessing import StandardScaler
import requests
from src.config.config import config

class PredictionService:
    """Servicio de predicción de ventas usando Machine Learning"""

    def __init__(self):
        self.model = None
        self.scaler = StandardScaler()
        self.java_url = config.JAVA_URL
        self._modelo_entrenado = False
        self._ultimo_entrenamiento = None

    def _obtener_pedidos_entregados(self):
        """Obtener pedidos entregados desde Java"""
        try:
            print(" Obteniendo pedidos desde Java...")
            response = requests.get(
                f'{self.java_url}/admin/data/pedidos/entregados',
                timeout=10
            )
            if response.status_code == 200:
                pedidos = response.json()
                print(f" Pedidos obtenidos: {len(pedidos)}")
                return pedidos
            else:
                print(f" Error obteniendo pedidos: {response.status_code}")
                return []
        except Exception as e:
            print(f" Error obteniendo pedidos: {e}")
            return []

    def preparar_datos(self, pedidos):
        """Preparar datos para el modelo"""
        if not pedidos:
            print(" No hay pedidos para preparar datos")
            return None, None, None

        datos = []
        for p in pedidos:
            fecha_str = p.get('fecha')
            if fecha_str:
                try:
                    if 'Z' in fecha_str:
                        fecha = datetime.fromisoformat(fecha_str.replace('Z', '+00:00'))
                    else:
                        fecha = datetime.fromisoformat(fecha_str)

                    datos.append({
                        'fecha': fecha,
                        'total': p.get('total', 0),
                        'dia_semana': fecha.weekday(),
                        'dia_mes': fecha.day,
                        'mes': fecha.month,
                        'es_fin_semana': 1 if fecha.weekday() >= 5 else 0
                    })
                except:
                    continue

        if len(datos) < 7:
            print(f" Datos insuficientes: {len(datos)} días (mínimo 7)")
            return None, None, None

        df = pd.DataFrame(datos)
        df = df.sort_values('fecha')

        print(f" Datos preparados: {len(df)} días")

        features = ['dia_semana', 'dia_mes', 'mes', 'es_fin_semana']
        X = df[features].values
        y = df['total'].values

        return X, y, df

    def entrenar(self):
        """Entrenar modelo con datos históricos"""
        try:
            print(" Iniciando entrenamiento...")
            pedidos = self._obtener_pedidos_entregados()
            X, y, df = self.preparar_datos(pedidos)

            if X is None or len(X) < 7:
                return {
                    'success': False,
                    'message': 'No hay suficientes datos para entrenar (mínimo 7 días)'
                }

            print(f" Entrenando con {len(X)} muestras...")
            X_scaled = self.scaler.fit_transform(X)
            self.model = LinearRegression()
            self.model.fit(X_scaled, y)
            score = self.model.score(X_scaled, y)
            self._modelo_entrenado = True
            self._ultimo_entrenamiento = datetime.now()

            print(f" Modelo entrenado. Precisión R²: {score:.4f}")
            return {
                'success': True,
                'message': 'Modelo entrenado exitosamente',
                'precision': round(score * 100, 2),
                'datos_entrenamiento': len(X)
            }
        except Exception as e:
            print(f" Error entrenando modelo: {e}")
            return {
                'success': False,
                'message': f'Error entrenando modelo: {str(e)}'
            }

    def predecir(self, dias=7):
        """Predecir ventas para los próximos X días usando ML"""
        #  Si el modelo no está entrenado, intentar entrenar
        if not self._modelo_entrenado or self.model is None:
            print(" Modelo no entrenado, intentando entrenar...")
            resultado = self.entrenar()
            if not resultado.get('success'):
                print(" No se pudo entrenar, usando método simple")
                return self.predecir_simple(dias)

        try:
            print(f"🔮 Generando predicciones ML para {dias} días...")
            predicciones = []
            hoy = datetime.now()
            dias_semana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']

            for i in range(dias):
                fecha = hoy + timedelta(days=i)
                features = np.array([[
                    fecha.weekday(),
                    fecha.day,
                    fecha.month,
                    1 if fecha.weekday() >= 5 else 0
                ]])

                features_scaled = self.scaler.transform(features)
                prediccion = self.model.predict(features_scaled)[0]

                # Asegurar que sea positivo
                prediccion = max(0, prediccion)

                # Calcular confianza basada en el R²
                confianza = 'Alta' if self.model.score(self.scaler.transform(np.ones((1, 4))), [0]) > 0.7 else 'Media'

                predicciones.append({
                    'fecha': fecha.strftime('%Y-%m-%d'),
                    'dia': dias_semana[fecha.weekday()],
                    'ventas_estimadas': round(prediccion, 2),
                    'confianza': confianza
                })

            return {
                'success': True,
                'predicciones': predicciones,
                'total_estimado': round(sum(p['ventas_estimadas'] for p in predicciones), 2),
                'promedio_diario': round(sum(p['ventas_estimadas'] for p in predicciones) / dias, 2),
                'metodo': 'ML'
            }
        except Exception as e:
            print(f" Error en predicción ML: {e}")
            return self.predecir_simple(dias)

    def predecir_simple(self, dias=7):
        """Método simple de predicción basado en promedio"""
        try:
            print(f" Usando método simple para {dias} días...")
            pedidos = self._obtener_pedidos_entregados()

            if not pedidos:
                print(" No hay pedidos para predecir")
                # Generar predicciones ficticias
                predicciones = []
                hoy = datetime.now()
                dias_semana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']

                for i in range(dias):
                    fecha = hoy + timedelta(days=i)
                    valor = 50 + np.random.normal(0, 10)  # Valor base ficticio
                    valor = max(0, valor)
                    predicciones.append({
                        'fecha': fecha.strftime('%Y-%m-%d'),
                        'dia': dias_semana[fecha.weekday()],
                        'ventas_estimadas': round(valor, 2),
                        'confianza': 'Baja (sin datos históricos)'
                    })

                return {
                    'success': True,
                    'predicciones': predicciones,
                    'total_estimado': round(sum(p['ventas_estimadas'] for p in predicciones), 2),
                    'promedio_diario': round(sum(p['ventas_estimadas'] for p in predicciones) / dias, 2),
                    'metodo': 'simple_sin_datos'
                }

            # Calcular promedio de ventas
            total_ventas = sum(p.get('total', 0) for p in pedidos)
            promedio = total_ventas / len(pedidos) if pedidos else 0

            print(f" Promedio de ventas: S/ {promedio:.2f}")

            predicciones = []
            hoy = datetime.now()
            dias_semana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']

            for i in range(dias):
                fecha = hoy + timedelta(days=i)
                # Agregar variación aleatoria para simular días diferentes
                variacion = np.random.normal(0, promedio * 0.15)
                valor = max(0, promedio + variacion)

                predicciones.append({
                    'fecha': fecha.strftime('%Y-%m-%d'),
                    'dia': dias_semana[fecha.weekday()],
                    'ventas_estimadas': round(valor, 2),
                    'confianza': 'Baja (predicción simple)'
                })

            return {
                'success': True,
                'predicciones': predicciones,
                'total_estimado': round(sum(p['ventas_estimadas'] for p in predicciones), 2),
                'promedio_diario': round(sum(p['ventas_estimadas'] for p in predicciones) / dias, 2),
                'metodo': 'simple'
            }
        except Exception as e:
            print(f" Error en predicción simple: {e}")
            return {
                'success': False,
                'message': f'Error en predicción simple: {str(e)}'
            }

    def analizar_tendencias(self):
        """Analizar tendencias en los datos históricos"""
        try:
            print(" Analizando tendencias...")
            pedidos = self._obtener_pedidos_entregados()
            X, y, df = self.preparar_datos(pedidos)

            if df is None or len(df) < 7:
                return {
                    'success': False,
                    'message': 'No hay suficientes datos para analizar (mínimo 7 días)'
                }

            # Análisis por día de semana
            ventas_por_dia = df.groupby('dia_semana')['total'].agg(['mean', 'sum', 'count']).reset_index()
            dias_nombres = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']
            ventas_por_dia['dia_nombre'] = ventas_por_dia['dia_semana'].apply(lambda x: dias_nombres[x])
            dia_max = ventas_por_dia.loc[ventas_por_dia['mean'].idxmax()]

            # Tendencia general usando regresión lineal simple
            df['dias_num'] = range(len(df))
            from sklearn.linear_model import LinearRegression as SimpleLR
            simple_model = SimpleLR()
            simple_model.fit(df[['dias_num']], df['total'])

            tendencia = 'estable'
            if simple_model.coef_[0] > 5:
                tendencia = 'creciente '
            elif simple_model.coef_[0] < -5:
                tendencia = 'decreciente '

            print(f" Tendencia analizada: {tendencia}")

            return {
                'success': True,
                'ventas_por_dia': ventas_por_dia.to_dict('records'),
                'dia_mas_vendido': {
                    'dia': dia_max['dia_nombre'],
                    'promedio': round(dia_max['mean'], 2),
                    'total': round(dia_max['sum'], 2)
                },
                'tendencia': tendencia,
                'pendiente': round(simple_model.coef_[0], 2),
                'total_dias': len(df),
                'venta_promedio_diaria': round(df['total'].mean(), 2)
            }
        except Exception as e:
            print(f" Error analizando tendencias: {e}")
            return {
                'success': False,
                'message': f'Error analizando tendencias: {str(e)}'
            }