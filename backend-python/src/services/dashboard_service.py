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

            print(f"📊 Datos obtenidos de Java:")
            print(f"   - Productos: {len(productos)}")
            print(f"   - Usuarios: {len(usuarios)}")
            print(f"   - Pedidos totales: {len(pedidos)}")

            hoy = datetime.now().date()
            hoy_inicio = datetime.combine(hoy, datetime.min.time())
            hoy_fin = datetime.combine(hoy, datetime.max.time())

            #  Filtrar pedidos PAGADOS o ENTREGADOS (ventas)
            pedidos_ventas = []
            for p in pedidos:
                estado = p.get('estado', '')
                if estado in ['PAGADO', 'ENTREGADO', 'CONFIRMADO']:
                    pedidos_ventas.append(p)
            pedidos_ventas = pedidos_ventas
            print(f"   - Pedidos pagados/entregados: {len(pedidos_ventas)}")

            # Pedidos de hoy (PAGADOS o ENTREGADOS)
            pedidos_hoy = []
            for p in pedidos_ventas:
                fecha_str = p.get('fecha')
                if fecha_str:
                    try:
                        fecha_limpia = fecha_str.replace('Z', '+00:00')
                        if 'T' in fecha_limpia:
                            fecha_limpia = fecha_limpia.split('T')[0]
                        if '.' in fecha_limpia:
                            fecha_limpia = fecha_limpia.split('.')[0]
                        fecha_pedido = datetime.strptime(fecha_limpia, '%Y-%m-%d').date()

                        if fecha_pedido == hoy:
                            pedidos_hoy.append(p)
                    except Exception as e:
                        print(f"⚠️ Error parseando fecha: {fecha_str} - {e}")

            ingresos_hoy = sum(p.get('total', 0) for p in pedidos_hoy)

            # Pedidos del mes (PAGADOS o ENTREGADOS)
            inicio_mes = datetime(hoy.year, hoy.month, 1).date()
            pedidos_mes = []
            for p in pedidos_ventas:
                fecha_str = p.get('fecha')
                if fecha_str:
                    try:
                        fecha_limpia = fecha_str.replace('Z', '+00:00')
                        if 'T' in fecha_limpia:
                            fecha_limpia = fecha_limpia.split('T')[0]
                        if '.' in fecha_limpia:
                            fecha_limpia = fecha_limpia.split('.')[0]
                        fecha_pedido = datetime.strptime(fecha_limpia, '%Y-%m-%d').date()

                        if fecha_pedido >= inicio_mes:
                            pedidos_mes.append(p)
                    except:
                        pass

            ventas_mes_total = sum(p.get('total', 0) for p in pedidos_mes)

            # Venta máxima
            ventas_entregadas = [p.get('total', 0) for p in pedidos_ventas]
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

            print(f"📈 Estadísticas calculadas:")
            print(f"   - Total Productos: {resultado['totalProductos']}")
            print(f"   - Total Usuarios: {resultado['totalUsuarios']}")
            print(f"   - Pedidos Hoy: {resultado['pedidosHoy']}")
            print(f"   - Ingresos Hoy: {resultado['ingresosHoy']}")

            return resultado

        except Exception as e:
            print(f"❌ Error en obtener_estadisticas_dashboard: {e}")
            import traceback
            traceback.print_exc()
            return {
                'success': False,
                'error': str(e)
            }


    #  VENTAS RECIENTES - INCLUYE CLIENTES PRESENCIALES


    def obtener_ventas_recientes(self, limit=10):
        """Obtener ventas recientes con datos de clientes (incluye presenciales)"""
        try:
            #  Obtener TODOS los pedidos
            pedidos = self.java_repo.get_pedidos()

            #  Filtrar pedidos pagados o entregados
            pedidos_filtrados = []
            for p in pedidos:
                estado = p.get('estado', '')
                if estado in ['PAGADO', 'ENTREGADO', 'CONFIRMADO']:
                    pedidos_filtrados.append(p)
            pedidos = pedidos_filtrados

            # Ordenar por fecha descendente
            pedidos.sort(key=lambda p: p.get('fecha', ''), reverse=True)

            # Tomar los más recientes
            pedidos_recientes = pedidos[:limit]

            ventas = []
            for p in pedidos_recientes:
                #  Obtener cliente correctamente
                cliente = 'Cliente general'

                #  Si tiene usuario registrado
                if p.get('usuario'):
                    usuario = p.get('usuario', {})
                    nombre = f"{usuario.get('nombres', '')} {usuario.get('apellidos', '')}".strip()
                    cliente = nombre if nombre else usuario.get('username', 'Cliente general')
                #  Si es cliente presencial (cajero)
                elif p.get('nombreCliente'):
                    cliente = p.get('nombreCliente', '').strip()
                #  Si tiene campo 'cliente' directo
                elif p.get('cliente'):
                    cliente = p.get('cliente', 'Cliente general')

                #  Obtener productos
                productos_nombres = []
                for item in p.get('items', []):
                    nombre = item.get('nombreProducto', 'Producto')
                    if nombre:
                        productos_nombres.append(nombre)

                #  Formatear fecha
                fecha_str = p.get('fecha', '')
                fecha_formateada = fecha_str
                if fecha_str:
                    try:
                        fecha_limpia = fecha_str.replace('Z', '+00:00')
                        if 'T' in fecha_limpia:
                            fecha_limpia = fecha_limpia.split('T')[0]
                        if '.' in fecha_limpia:
                            fecha_limpia = fecha_limpia.split('.')[0]
                        fecha_obj = datetime.strptime(fecha_limpia, '%Y-%m-%d')
                        fecha_formateada = fecha_obj.strftime('%d/%m/%Y %I:%M %p')
                    except:
                        pass

                ventas.append({
                    'id': p.get('id'),
                    'numeroPedido': p.get('numeroPedido', 'N/A'),
                    'cliente': cliente,
                    'productos': ', '.join(productos_nombres[:3]) + ('...' if len(productos_nombres) > 3 else ''),
                    'total': round(p.get('total', 0), 2),
                    'fecha': fecha_formateada,
                    'estado': p.get('estado', 'PAGADO')
                })

            return ventas

        except Exception as e:
            print(f"❌ Error en obtener_ventas_recientes: {e}")
            import traceback
            traceback.print_exc()
            return []


    #  ESTADÍSTICAS DE VENTAS


    def obtener_estadisticas_ventas(self):
        """Estadísticas de ventas por día (última semana)"""
        try:

            pedidos = self.java_repo.get_pedidos()


            pedidos_filtrados = []
            for p in pedidos:
                estado = p.get('estado', '')
                if estado in ['PAGADO', 'ENTREGADO', 'CONFIRMADO']:
                    pedidos_filtrados.append(p)
            pedidos = pedidos_filtrados

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
                            fecha_limpia = fecha_pedido_str.replace('Z', '+00:00')
                            if 'T' in fecha_limpia:
                                fecha_limpia = fecha_limpia.split('T')[0]
                            if '.' in fecha_limpia:
                                fecha_limpia = fecha_limpia.split('.')[0]
                            fecha_pedido = datetime.strptime(fecha_limpia, '%Y-%m-%d').date()

                            if inicio_dia.date() <= fecha_pedido <= fin_dia.date():
                                ventas_dia.append(p)
                        except:
                            pass

                ventas_por_dia[fecha_str] = round(sum(p.get('total', 0) for p in ventas_dia), 2)

            return {
                'success': True,
                'ventasPorDia': ventas_por_dia
            }

        except Exception as e:
            print(f"❌ Error en obtener_estadisticas_ventas: {e}")
            return {
                'success': False,
                'error': str(e)
            }