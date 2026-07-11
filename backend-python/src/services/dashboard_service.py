from datetime import datetime, timedelta
from src.repositories.java_repository import JavaRepository

class DashboardService:
    """Servicio para estadísticas del dashboard"""

    def __init__(self):
        self.java_repo = JavaRepository()

    def obtener_estadisticas_dashboard(self):
        """Calcular estadísticas del dashboard desde datos de Java"""
        try:
            # Obtener datos de Java
            productos = self.java_repo.get_productos()
            usuarios = self.java_repo.get_usuarios()
            pedidos = self.java_repo.get_pedidos()

            print(f" Datos obtenidos de Java:")
            print(f"   - Productos: {len(productos)}")
            print(f"   - Usuarios: {len(usuarios)}")
            print(f"   - Pedidos totales: {len(pedidos)}")

            hoy = datetime.now().date()
            hoy_inicio = datetime.combine(hoy, datetime.min.time())
            hoy_fin = datetime.combine(hoy, datetime.max.time())

            # Filtrar pedidos ENTREGADOS (ventas)
            pedidos_entregados = [p for p in pedidos if p.get('estado') == 'ENTREGADO']
            print(f"   - Pedidos entregados: {len(pedidos_entregados)}")

            # Pedidos de hoy (ENTREGADOS)
            pedidos_hoy = []
            for p in pedidos_entregados:
                fecha_str = p.get('fecha')
                if fecha_str:
                    try:
                        # Manejar diferentes formatos de fecha
                        if 'Z' in fecha_str:
                            fecha_pedido = datetime.fromisoformat(fecha_str.replace('Z', '+00:00'))
                        else:
                            fecha_pedido = datetime.fromisoformat(fecha_str)

                        if hoy_inicio <= fecha_pedido <= hoy_fin:
                            pedidos_hoy.append(p)
                    except Exception as e:
                        print(f"⚠️ Error parseando fecha: {fecha_str} - {e}")

            ingresos_hoy = sum(p.get('total', 0) for p in pedidos_hoy)

            # Pedidos del mes (ENTREGADOS)
            inicio_mes = datetime(hoy.year, hoy.month, 1)
            pedidos_mes = []
            for p in pedidos_entregados:
                fecha_str = p.get('fecha')
                if fecha_str:
                    try:
                        if 'Z' in fecha_str:
                            fecha_pedido = datetime.fromisoformat(fecha_str.replace('Z', '+00:00'))
                        else:
                            fecha_pedido = datetime.fromisoformat(fecha_str)

                        if fecha_pedido >= inicio_mes:
                            pedidos_mes.append(p)
                    except:
                        pass

            ventas_mes_total = sum(p.get('total', 0) for p in pedidos_mes)

            # Venta máxima
            ventas_entregadas = [p.get('total', 0) for p in pedidos_entregados]
            venta_maxima = max(ventas_entregadas) if ventas_entregadas else 0.0

            # Promedio diario
            dia_mes = hoy.day
            promedio_diario = ventas_mes_total / dia_mes if dia_mes > 0 else 0.0

            # Total de pedidos (todos, no solo entregados)
            total_pedidos = len(pedidos)

            resultado = {
                'success': True,
                'totalProductos': len(productos),
                'totalUsuarios': len(usuarios),
                'pedidosHoy': len(pedidos_hoy),
                'ingresosHoy': round(ingresos_hoy, 2),
                'ventasMesTotal': round(ventas_mes_total, 2),
                'totalPedidos': total_pedidos,
                'ventaMaxima': round(venta_maxima, 2),
                'promedioDiario': round(promedio_diario, 2)
            }

            print(f" Estadísticas calculadas:")
            print(f"   - Total Productos: {resultado['totalProductos']}")
            print(f"   - Total Usuarios: {resultado['totalUsuarios']}")
            print(f"   - Pedidos Hoy: {resultado['pedidosHoy']}")
            print(f"   - Ingresos Hoy: {resultado['ingresosHoy']}")

            return resultado

        except Exception as e:
            print(f" Error en obtener_estadisticas_dashboard: {e}")
            import traceback
            traceback.print_exc()
            return {
                'success': False,
                'error': str(e)
            }

    def obtener_ventas_recientes(self, limit=10):
        """Obtener ventas recientes"""
        try:
            pedidos = self.java_repo.get_pedidos_entregados()
            # Ordenar por fecha descendente
            pedidos.sort(key=lambda p: p.get('fecha', ''), reverse=True)
            return pedidos[:limit]
        except Exception as e:
            print(f" Error en obtener_ventas_recientes: {e}")
            return []

    def obtener_estadisticas_ventas(self):
        """Estadísticas de ventas por día (última semana)"""
        try:
            pedidos = self.java_repo.get_pedidos_entregados()

            hoy = datetime.now().date()
            ventas_por_dia = {}

            for i in range(6, -1, -1):
                fecha = hoy - timedelta(days=i)
                fecha_str = fecha.strftime("%d/%m")

                inicio_dia = datetime.combine(fecha, datetime.min.time())
                fin_dia = datetime.combine(fecha, datetime.max.time())

                ventas_dia = []
                for p in pedidos:
                    fecha_pedido_str = p.get('fecha')
                    if fecha_pedido_str:
                        try:
                            if 'Z' in fecha_pedido_str:
                                fecha_pedido = datetime.fromisoformat(fecha_pedido_str.replace('Z', '+00:00'))
                            else:
                                fecha_pedido = datetime.fromisoformat(fecha_pedido_str)

                            if inicio_dia <= fecha_pedido <= fin_dia:
                                ventas_dia.append(p)
                        except:
                            pass

                ventas_por_dia[fecha_str] = round(sum(p.get('total', 0) for p in ventas_dia), 2)

            return {
                'success': True,
                'ventasPorDia': ventas_por_dia
            }

        except Exception as e:
            print(f" Error en obtener_estadisticas_ventas: {e}")
            return {
                'success': False,
                'error': str(e)
            }