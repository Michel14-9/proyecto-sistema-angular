# src/services/reportes_service.py
import requests
from datetime import datetime, timedelta
from src.config.config import config
from collections import defaultdict

class ReportesService:
    """Servicio para reportes y estadísticas avanzadas"""

    def __init__(self):
        self.java_url = config.JAVA_URL
        self._productos_cache = None
        self._cache_timestamp = None

    def _obtener_pedidos(self):
        """Obtener pedidos desde Java"""
        response = requests.get(
            f'{self.java_url}/admin/data/pedidos',
            timeout=10
        )
        return response.json() if response.status_code == 200 else []

    def _obtener_productos(self):
        """Obtener productos desde Java con caché"""
        if self._productos_cache and self._cache_timestamp:
            if (datetime.now() - self._cache_timestamp).seconds < 60:
                return self._productos_cache

        response = requests.get(
            f'{self.java_url}/admin/data/productos',
            timeout=10
        )
        if response.status_code == 200:
            self._productos_cache = response.json()
            self._cache_timestamp = datetime.now()
            return self._productos_cache
        return []

    def _obtener_usuarios(self):
        """Obtener usuarios desde Java"""
        response = requests.get(
            f'{self.java_url}/admin/data/usuarios',
            timeout=10
        )
        return response.json() if response.status_code == 200 else []

    def _obtener_categoria_producto(self, nombre_producto):
        """Obtener categoría de un producto por su nombre"""
        productos = self._obtener_productos()
        for p in productos:
            if p.get('nombre') == nombre_producto:
                categoria = p.get('tipo', 'Otros')
                if not categoria or categoria == 'Sin categoría':
                    return 'Otros'
                return categoria
        return 'Otros'

    def _determinar_categoria_por_nombre(self, nombre):
        """Determinar categoría basada en el nombre del producto (fallback)"""
        nombre_lower = nombre.lower()
        if any(palabra in nombre_lower for palabra in ['pollo', 'broaster', 'pollo a la brasa', 'pollo asado']):
            return 'Pollos'
        elif any(palabra in nombre_lower for palabra in ['parrilla', 'churrasco', 'asado', 'parrillada']):
            return 'Parrillas'
        elif any(palabra in nombre_lower for palabra in ['chicharrón', 'chicharron']):
            return 'Chicharrón'
        elif any(palabra in nombre_lower for palabra in ['hamburguesa', 'burger', 'sandwich']):
            return 'Hamburguesas'
        elif any(palabra in nombre_lower for palabra in ['combo', 'familiar', 'pack', 'promo']):
            return 'Combos'
        elif any(palabra in nombre_lower for palabra in ['criollo', 'guiso', 'estofado', 'aji']):
            return 'Criollos'
        else:
            return 'Otros'

    # ============================================================
    # OBTENER REPORTE DE VENTAS
    # ============================================================

    def obtener_reporte_ventas(self, fecha_inicio=None, fecha_fin=None, tipo='ventas'):
        """Obtener reporte de ventas con filtro de fechas y tipo"""
        try:
            # Obtener TODOS los pedidos
            response = requests.get(
                f'{self.java_url}/admin/data/pedidos',
                timeout=10
            )

            if response.status_code != 200:
                return {
                    'success': False,
                    'error': f'Error al obtener pedidos: {response.status_code}'
                }

            pedidos = response.json()

            # Filtrar SOLO pedidos pagados o entregados
            pedidos_filtrados = []
            for p in pedidos:
                estado = p.get('estado', '')
                if estado in ['PAGADO', 'ENTREGADO', 'CONFIRMADO']:
                    pedidos_filtrados.append(p)
            pedidos = pedidos_filtrados

            # Filtrar por fechas
            if fecha_inicio and fecha_fin:
                try:
                    print(f"📅 Filtrando por fechas: {fecha_inicio} - {fecha_fin}")

                    inicio = datetime.strptime(fecha_inicio, '%Y-%m-%d')
                    fin = datetime.strptime(fecha_fin, '%Y-%m-%d')
                    fin = fin.replace(hour=23, minute=59, second=59)

                    print(f"📅 Fechas parseadas: inicio={inicio}, fin={fin}")

                    pedidos_filtrados_temp = []
                    for p in pedidos:
                        fecha_pedido_str = p.get('fecha')
                        if fecha_pedido_str:
                            try:
                                fecha_limpia = fecha_pedido_str.replace('Z', '+00:00')
                                if 'T' in fecha_limpia:
                                    fecha_limpia = fecha_limpia.split('T')[0]
                                if '.' in fecha_limpia:
                                    fecha_limpia = fecha_limpia.split('.')[0]

                                fecha_pedido = datetime.strptime(fecha_limpia, '%Y-%m-%d')

                                if inicio <= fecha_pedido <= fin:
                                    pedidos_filtrados_temp.append(p)
                            except Exception as e:
                                print(f"⚠️ Error parseando fecha {fecha_pedido_str}: {e}")
                                continue

                    print(f"📊 Total pedidos después de filtrar: {len(pedidos_filtrados_temp)}")
                    pedidos = pedidos_filtrados_temp

                except Exception as e:
                    print(f"❌ Error filtrando fechas: {e}")
                    import traceback
                    traceback.print_exc()

            # PROCESAR SEGÚN EL TIPO DE REPORTE
            if tipo == 'productos':
                return self._procesar_productos(pedidos, fecha_inicio, fecha_fin)
            elif tipo == 'pedidos':
                return self._procesar_pedidos(pedidos, fecha_inicio, fecha_fin)
            elif tipo == 'usuarios':
                return self._procesar_usuarios(pedidos, fecha_inicio, fecha_fin)
            elif tipo == 'metodos-pago':
                return self._procesar_metodos_pago(pedidos, fecha_inicio, fecha_fin)
            elif tipo == 'tipos-entrega':
                return self._procesar_tipos_entrega(pedidos, fecha_inicio, fecha_fin)
            elif tipo == 'horarios':
                return self._procesar_horarios_pico(pedidos, fecha_inicio, fecha_fin)
            elif tipo == 'favoritos':
                return self._procesar_favoritos(fecha_inicio, fecha_fin)
            else:
                return self._procesar_ventas(pedidos, fecha_inicio, fecha_fin)

        except Exception as e:
            print(f"❌ Error en obtener_reporte_ventas: {e}")
            import traceback
            traceback.print_exc()
            return {
                'success': False,
                'error': str(e)
            }

    # ============================================================
    # PROCESAR VENTAS (INCLUYE CLIENTES PRESENCIALES)
    # ============================================================

    def _procesar_ventas(self, pedidos, fecha_inicio, fecha_fin):
        """Procesar reporte de VENTAS (gráfico de líneas con fechas)"""
        ventas_por_dia = defaultdict(float)
        pedidos_por_dia = defaultdict(int)

        for p in pedidos:
            fecha_str = p.get('fecha', '')
            if fecha_str:
                try:
                    if 'T' in fecha_str:
                        fecha_solo = fecha_str.split('T')[0]
                    elif ' ' in fecha_str:
                        fecha_solo = fecha_str.split(' ')[0]
                    else:
                        fecha_solo = fecha_str[:10]

                    ventas_por_dia[fecha_solo] += p.get('total', 0)
                    pedidos_por_dia[fecha_solo] += 1
                except:
                    continue

        fechas_ordenadas = sorted(ventas_por_dia.keys())

        datos_grafico = []
        for fecha in fechas_ordenadas:
            try:
                fecha_obj = datetime.strptime(fecha, '%Y-%m-%d')
                fecha_formateada = fecha_obj.strftime('%d/%m')
            except:
                fecha_formateada = fecha

            datos_grafico.append({
                'fecha': fecha_formateada,
                'fecha_completa': fecha,
                'total': ventas_por_dia[fecha],
                'pedidos': pedidos_por_dia[fecha]
            })

        total_ventas = sum(p.get('total', 0) for p in pedidos)
        total_pedidos = len(pedidos)
        productos_vendidos = 0
        for p in pedidos:
            for item in p.get('items', []):
                productos_vendidos += item.get('cantidad', 0)

        crecimiento = 0
        if len(datos_grafico) >= 2:
            mitad = len(datos_grafico) // 2
            primera_mitad = sum(d['total'] for d in datos_grafico[:mitad])
            segunda_mitad = sum(d['total'] for d in datos_grafico[mitad:])
            if primera_mitad > 0:
                crecimiento = ((segunda_mitad - primera_mitad) / primera_mitad) * 100


        tabla_datos = []
        for p in pedidos[:50]:
            cliente = 'Cliente general'


            if p.get('usuario'):
                usuario = p.get('usuario', {})
                cliente = f"{usuario.get('nombres', '')} {usuario.get('apellidos', '')}".strip()
                if not cliente:
                    cliente = usuario.get('username', 'Cliente general')
            elif p.get('nombreCliente'):
                cliente = p.get('nombreCliente', '').strip()
            elif p.get('cliente'):
                cliente = p.get('cliente', 'Cliente general')

            productos_nombres = []
            for item in p.get('items', []):
                nombre = item.get('nombreProducto', 'Producto')
                productos_nombres.append(nombre)

            tabla_datos.append({
                'id': p.get('id'),
                'fecha': p.get('fecha'),
                'cliente': cliente,
                'productos': ', '.join(productos_nombres[:3]) + ('...' if len(productos_nombres) > 3 else ''),
                'total': p.get('total', 0),
                'estado': p.get('estado', 'ENTREGADO')
            })

        categorias_map = defaultdict(float)
        for p in pedidos:
            for item in p.get('items', []):
                nombre = item.get('nombreProducto', 'Producto')
                subtotal = item.get('subtotal', 0)
                categoria = self._obtener_categoria_producto(nombre)
                if not categoria or categoria == 'Sin categoría':
                    categoria = self._determinar_categoria_por_nombre(nombre)
                categorias_map[categoria] += subtotal

        if not categorias_map or sum(categorias_map.values()) == 0:
            categorias_map = {
                'Pollos': 0,
                'Parrillas': 0,
                'Chicharrón': 0,
                'Broaster': 0,
                'Hamburguesas': 0,
                'Criollos': 0,
                'Combos': 0
            }

        return {
            'success': True,
            'metricas': {
                'totalVentas': round(total_ventas, 2),
                'totalPedidos': total_pedidos,
                'productosVendidos': productos_vendidos,
                'crecimiento': round(crecimiento, 2)
            },
            'data': tabla_datos,
            'datos_grafico': {
                'labels': [d['fecha'] for d in datos_grafico],
                'datos': [d['total'] for d in datos_grafico],
                'pedidos': [d['pedidos'] for d in datos_grafico]
            },
            'categorias': {
                'labels': list(categorias_map.keys()),
                'datos': list(categorias_map.values())
            }
        }

    # ============================================================
    # PROCESAR PRODUCTOS
    # ============================================================

    def _procesar_productos(self, pedidos, fecha_inicio, fecha_fin):
        """Procesar reporte de PRODUCTOS MÁS VENDIDOS"""
        productos_map = defaultdict(lambda: {'cantidad': 0, 'monto': 0})

        for p in pedidos:
            for item in p.get('items', []):
                nombre = item.get('nombreProducto', 'Producto')
                cantidad = item.get('cantidad', 0)
                subtotal = item.get('subtotal', 0)

                productos_map[nombre]['cantidad'] += cantidad
                productos_map[nombre]['monto'] += subtotal

        productos_ordenados = sorted(
            productos_map.items(),
            key=lambda x: x[1]['cantidad'],
            reverse=True
        )[:10]

        labels = [p[0] for p in productos_ordenados]
        datos = [p[1]['cantidad'] for p in productos_ordenados]

        categorias_map = defaultdict(float)
        for nombre, data in productos_map.items():
            categoria = self._obtener_categoria_producto(nombre)
            if not categoria or categoria == 'Sin categoría':
                categoria = self._determinar_categoria_por_nombre(nombre)
            categorias_map[categoria] += data['monto']

        if not categorias_map or sum(categorias_map.values()) == 0:
            categorias_map = {
                'Pollos': 0,
                'Parrillas': 0,
                'Chicharrón': 0,
                'Broaster': 0,
                'Hamburguesas': 0,
                'Criollos': 0,
                'Combos': 0
            }

        tabla_datos = []
        for nombre, data in productos_ordenados:
            tabla_datos.append({
                'producto': nombre,
                'cantidad': data['cantidad'],
                'monto': round(data['monto'], 2)
            })

        return {
            'success': True,
            'metricas': {
                'totalVentas': round(sum(p[1]['monto'] for p in productos_ordenados), 2),
                'totalPedidos': len(pedidos),
                'productosVendidos': sum(p[1]['cantidad'] for p in productos_ordenados),
                'crecimiento': 0
            },
            'data': tabla_datos,
            'datos_grafico': {
                'labels': labels,
                'datos': datos,
                'pedidos': datos
            },
            'categorias': {
                'labels': list(categorias_map.keys()),
                'datos': list(categorias_map.values())
            }
        }

    # ============================================================
    # PROCESAR PEDIDOS
    # ============================================================

    def _procesar_pedidos(self, pedidos, fecha_inicio, fecha_fin):
        """Procesar reporte de ESTADÍSTICAS DE PEDIDOS"""
        estados_map = defaultdict(int)
        for p in pedidos:
            estado = p.get('estado', 'PENDIENTE')
            estados_map[estado] += 1

        labels = list(estados_map.keys())
        datos = list(estados_map.values())

        tabla_datos = []
        for estado, cantidad in estados_map.items():
            tabla_datos.append({
                'estado': estado,
                'cantidad': cantidad
            })

        return {
            'success': True,
            'metricas': {
                'totalVentas': round(sum(p.get('total', 0) for p in pedidos), 2),
                'totalPedidos': len(pedidos),
                'productosVendidos': 0,
                'crecimiento': 0
            },
            'data': tabla_datos,
            'datos_grafico': {
                'labels': labels,
                'datos': datos,
                'pedidos': datos
            },
            'categorias': {
                'labels': ['Completados', 'Pendientes'],
                'datos': [
                    estados_map.get('ENTREGADO', 0) + estados_map.get('PAGADO', 0),
                    estados_map.get('PENDIENTE', 0) + estados_map.get('PREPARACION', 0)
                ]
            }
        }


    # PROCESAR USUARIOS


    def _procesar_usuarios(self, pedidos, fecha_inicio, fecha_fin):
        """Procesar reporte de ACTIVIDAD DE USUARIOS"""
        usuarios_map = defaultdict(int)


        for p in pedidos:
            if p.get('usuario'):
                usuario = p.get('usuario', {})
                nombre = f"{usuario.get('nombres', '')} {usuario.get('apellidos', '')}".strip()
                if not nombre:
                    nombre = usuario.get('username', 'Usuario registrado')
                usuarios_map[nombre] += 1


        for p in pedidos:
            if p.get('usuario') is None and p.get('nombreCliente'):
                nombre = p.get('nombreCliente', '').strip()
                if nombre:
                    usuarios_map[nombre] += 1


        for p in pedidos:
            if p.get('usuario') is None and not p.get('nombreCliente'):
                usuarios_map['Cliente general'] += 1


        usuarios_registrados = self._obtener_usuarios()
        for usuario in usuarios_registrados:
            nombre = f"{usuario.get('nombres', '')} {usuario.get('apellidos', '')}".strip()
            if not nombre:
                nombre = usuario.get('username', 'Usuario')
            if nombre not in usuarios_map:
                usuarios_map[nombre] = 0

        usuarios_ordenados = sorted(
            usuarios_map.items(),
            key=lambda x: x[1],
            reverse=True
        )

        labels = [u[0] for u in usuarios_ordenados[:10]]
        datos = [u[1] for u in usuarios_ordenados[:10]]

        tabla_datos = []
        for nombre, pedidos_count in usuarios_ordenados:
            tabla_datos.append({
                'usuario': nombre,
                'pedidos': pedidos_count
            })

        total_clientes = len(usuarios_map)
        total_pedidos = sum(usuarios_map.values())
        clientes_con_pedidos = sum(1 for v in usuarios_map.values() if v > 0)
        clientes_sin_pedidos = total_clientes - clientes_con_pedidos

        return {
            'success': True,
            'metricas': {
                'totalVentas': round(sum(p.get('total', 0) for p in pedidos), 2),
                'totalPedidos': len(pedidos),
                'productosVendidos': 0,
                'crecimiento': 0,
                'totalClientes': total_clientes,
                'clientesConPedidos': clientes_con_pedidos,
                'clientesSinPedidos': clientes_sin_pedidos
            },
            'data': tabla_datos,
            'datos_grafico': {
                'labels': labels,
                'datos': datos,
                'pedidos': datos
            },
            'categorias': {
                'labels': ['Clientes activos', 'Clientes inactivos'],
                'datos': [clientes_con_pedidos, clientes_sin_pedidos]
            },
            'titulo': 'Actividad de Usuarios'
        }


    # MÉTODOS DE PAGO


    def _procesar_metodos_pago(self, pedidos, fecha_inicio, fecha_fin):
        """Reporte de MÉTODOS DE PAGO"""
        metodos_map = defaultdict(int)
        montos_map = defaultdict(float)

        for p in pedidos:
            metodo = p.get('metodoPago', 'No especificado')
            if not metodo or metodo == '':
                metodo = 'No especificado'
            metodos_map[metodo] += 1
            montos_map[metodo] += p.get('total', 0)

        metodos_ordenados = sorted(
            metodos_map.items(),
            key=lambda x: x[1],
            reverse=True
        )

        labels = [m[0] for m in metodos_ordenados]
        datos = [m[1] for m in metodos_ordenados]
        montos = [montos_map[m[0]] for m in metodos_ordenados]

        tabla_datos = []
        for metodo, cantidad in metodos_ordenados:
            tabla_datos.append({
                'metodo': metodo,
                'cantidad': cantidad,
                'monto': round(montos_map[metodo], 2)
            })

        return {
            'success': True,
            'metricas': {
                'totalVentas': round(sum(montos_map.values()), 2),
                'totalPedidos': len(pedidos),
                'metodosUsados': len(metodos_map),
                'crecimiento': 0
            },
            'data': tabla_datos,
            'datos_grafico': {
                'labels': labels,
                'datos': datos,
                'montos': montos
            },
            'categorias': {
                'labels': labels,
                'datos': montos
            },
            'titulo': 'Métodos de Pago'
        }


    # TIPOS DE ENTREGA


    def _procesar_tipos_entrega(self, pedidos, fecha_inicio, fecha_fin):
        """Reporte de TIPOS DE ENTREGA"""
        tipos_map = defaultdict(int)
        montos_map = defaultdict(float)

        for p in pedidos:
            tipo = p.get('tipoEntrega', 'No especificado')
            if not tipo or tipo == '':
                tipo = 'No especificado'
            tipos_map[tipo] += 1
            montos_map[tipo] += p.get('total', 0)

        tipos_ordenados = sorted(
            tipos_map.items(),
            key=lambda x: x[1],
            reverse=True
        )

        labels = [t[0] for t in tipos_ordenados]
        datos = [t[1] for t in tipos_ordenados]
        montos = [montos_map[t[0]] for t in tipos_ordenados]

        tabla_datos = []
        for tipo, cantidad in tipos_ordenados:
            tabla_datos.append({
                'tipo': tipo,
                'cantidad': cantidad,
                'monto': round(montos_map[tipo], 2)
            })

        return {
            'success': True,
            'metricas': {
                'totalVentas': round(sum(montos_map.values()), 2),
                'totalPedidos': len(pedidos),
                'tiposUsados': len(tipos_map),
                'crecimiento': 0
            },
            'data': tabla_datos,
            'datos_grafico': {
                'labels': labels,
                'datos': datos,
                'montos': montos
            },
            'categorias': {
                'labels': labels,
                'datos': montos
            },
            'titulo': 'Tipos de Entrega'
        }


    # HORARIOS PICO


    def _procesar_horarios_pico(self, pedidos, fecha_inicio, fecha_fin):
        """Reporte de HORARIOS PICO"""
        horas_map = defaultdict(int)
        montos_map = defaultdict(float)

        for p in pedidos:
            fecha_str = p.get('fecha', '')
            if fecha_str:
                try:
                    if 'T' in fecha_str:
                        fecha = datetime.fromisoformat(fecha_str.replace('Z', '+00:00'))
                    elif ' ' in fecha_str:
                        fecha = datetime.fromisoformat(fecha_str)
                    else:
                        continue

                    hora = fecha.hour
                    rango_hora = f"{hora:02d}:00 - {hora+1:02d}:00"
                    horas_map[rango_hora] += 1
                    montos_map[rango_hora] += p.get('total', 0)
                except:
                    continue

        horas_ordenadas = sorted(horas_map.items(), key=lambda x: x[0])

        labels = [h[0] for h in horas_ordenadas]
        datos = [h[1] for h in horas_ordenadas]
        montos = [montos_map[h[0]] for h in horas_ordenadas]

        hora_pico = max(horas_map.items(), key=lambda x: x[1]) if horas_map else ('No hay datos', 0)

        tabla_datos = []
        for hora, cantidad in horas_ordenadas:
            tabla_datos.append({
                'hora': hora,
                'pedidos': cantidad,
                'monto': round(montos_map[hora], 2)
            })

        return {
            'success': True,
            'metricas': {
                'totalPedidos': len(pedidos),
                'totalVentas': round(sum(montos_map.values()), 2),
                'horaPico': hora_pico[0],
                'pedidosHoraPico': hora_pico[1]
            },
            'data': tabla_datos,
            'datos_grafico': {
                'labels': labels,
                'datos': datos,
                'montos': montos
            },
            'categorias': {
                'labels': ['Mañana (6-12)', 'Tarde (12-18)', 'Noche (18-24)'],
                'datos': [
                    sum(montos_map[h] for h in horas_map.keys() if '06' in h or '07' in h or '08' in h or '09' in h or '10' in h or '11' in h),
                    sum(montos_map[h] for h in horas_map.keys() if '12' in h or '13' in h or '14' in h or '15' in h or '16' in h or '17' in h),
                    sum(montos_map[h] for h in horas_map.keys() if '18' in h or '19' in h or '20' in h or '21' in h or '22' in h or '23' in h)
                ]
            },
            'titulo': 'Horarios Pico'
        }


    # FAVORITOS


    def _procesar_favoritos(self, fecha_inicio, fecha_fin):
        """Reporte de PRODUCTOS FAVORITOS"""
        try:
            response = requests.get(
                f'{self.java_url}/admin/data/favoritos',
                timeout=10
            )

            if response.status_code != 200:
                print(f"⚠️ Error en favoritos: {response.status_code}")
                return self._procesar_productos_como_favoritos(fecha_inicio, fecha_fin)

            favoritos_data = response.json()

            if not favoritos_data:
                return {
                    'success': True,
                    'metricas': {
                        'totalFavoritos': 0,
                        'totalProductosFavoritos': 0,
                        'topProducto': 'N/A',
                        'topCantidad': 0
                    },
                    'data': [],
                    'datos_grafico': {
                        'labels': [],
                        'datos': [],
                        'pedidos': []
                    },
                    'categorias': {
                        'labels': ['Favoritos', 'No favoritos'],
                        'datos': [0, len(self._obtener_productos())]
                    },
                    'titulo': 'Productos Favoritos'
                }

            productos_map = defaultdict(int)
            for f in favoritos_data:
                nombre = f.get('productoNombre', 'Producto')
                if nombre:
                    productos_map[nombre] += 1

            productos_ordenados = sorted(
                productos_map.items(),
                key=lambda x: x[1],
                reverse=True
            )[:10]

            labels = [p[0] for p in productos_ordenados]
            datos = [p[1] for p in productos_ordenados]

            tabla_datos = []
            for nombre, cantidad in productos_ordenados:
                tabla_datos.append({
                    'producto': nombre,
                    'favoritos': cantidad
                })

            total_productos = len(self._obtener_productos())
            total_favoritos = sum(productos_map.values())
            productos_con_favoritos = len(productos_map)

            return {
                'success': True,
                'metricas': {
                    'totalFavoritos': total_favoritos,
                    'totalProductosFavoritos': productos_con_favoritos,
                    'topProducto': productos_ordenados[0][0] if productos_ordenados else 'N/A',
                    'topCantidad': productos_ordenados[0][1] if productos_ordenados else 0
                },
                'data': tabla_datos,
                'datos_grafico': {
                    'labels': labels,
                    'datos': datos,
                    'pedidos': datos
                },
                'categorias': {
                    'labels': ['Favoritos', 'No favoritos'],
                    'datos': [
                        total_favoritos,
                        max(0, total_productos - productos_con_favoritos)
                    ]
                },
                'titulo': 'Productos Favoritos'
            }

        except Exception as e:
            print(f"❌ Error en favoritos: {e}")
            return self._procesar_productos_como_favoritos(fecha_inicio, fecha_fin)


    # FALLBACK: PRODUCTOS COMO FAVORITOS


    def _procesar_productos_como_favoritos(self, fecha_inicio, fecha_fin):
        """Fallback: usar productos más vendidos como favoritos"""
        try:
            pedidos = self._obtener_pedidos()

            if not pedidos:
                return {
                    'success': True,
                    'metricas': {
                        'totalFavoritos': 0,
                        'totalProductosFavoritos': 0,
                        'topProducto': 'N/A',
                        'topCantidad': 0
                    },
                    'data': [],
                    'datos_grafico': {
                        'labels': [],
                        'datos': [],
                        'pedidos': []
                    },
                    'categorias': {
                        'labels': ['Favoritos', 'No favoritos'],
                        'datos': [0, len(self._obtener_productos())]
                    },
                    'titulo': 'Productos Favoritos (basado en ventas)'
                }

            productos_map = defaultdict(int)
            for p in pedidos:
                for item in p.get('items', []):
                    nombre = item.get('nombreProducto', 'Producto')
                    productos_map[nombre] += item.get('cantidad', 0)

            productos_ordenados = sorted(
                productos_map.items(),
                key=lambda x: x[1],
                reverse=True
            )[:10]

            labels = [p[0] for p in productos_ordenados]
            datos = [p[1] for p in productos_ordenados]
            total_productos = len(self._obtener_productos())

            return {
                'success': True,
                'metricas': {
                    'totalFavoritos': sum(productos_map.values()),
                    'totalProductosFavoritos': len(productos_map),
                    'topProducto': productos_ordenados[0][0] if productos_ordenados else 'N/A',
                    'topCantidad': productos_ordenados[0][1] if productos_ordenados else 0
                },
                'data': [{'producto': p[0], 'favoritos': p[1]} for p in productos_ordenados],
                'datos_grafico': {
                    'labels': labels,
                    'datos': datos,
                    'pedidos': datos
                },
                'categorias': {
                    'labels': ['Favoritos', 'No favoritos'],
                    'datos': [
                        sum(productos_map.values()),
                        max(0, total_productos - len(productos_map))
                    ]
                },
                'titulo': 'Productos Favoritos (basado en ventas)'
            }
        except Exception as e:
            return {
                'success': False,
                'error': str(e)
            }


    # REPORTE COMPLETO


    def obtener_reporte_completo(self, fecha_inicio=None, fecha_fin=None):
        """Obtener reporte completo con estadísticas avanzadas filtrado por fechas"""
        try:
            print(f"📊 Generando reporte completo con fechas: {fecha_inicio} - {fecha_fin}")

            productos = self._obtener_productos()
            usuarios = self._obtener_usuarios()
            pedidos = self._obtener_pedidos()

            # FILTRAR PEDIDOS POR FECHA
            if fecha_inicio and fecha_fin:
                try:
                    print(f"📅 Filtrando por fechas: {fecha_inicio} - {fecha_fin}")

                    inicio = datetime.strptime(fecha_inicio, '%Y-%m-%d')
                    fin = datetime.strptime(fecha_fin, '%Y-%m-%d')
                    fin = fin.replace(hour=23, minute=59, second=59)

                    pedidos_filtrados = []
                    for p in pedidos:
                        fecha_pedido_str = p.get('fecha')
                        if fecha_pedido_str:
                            try:
                                fecha_limpia = fecha_pedido_str.replace('Z', '+00:00')
                                if 'T' in fecha_limpia:
                                    fecha_limpia = fecha_limpia.split('T')[0]
                                if '.' in fecha_limpia:
                                    fecha_limpia = fecha_limpia.split('.')[0]

                                fecha_pedido = datetime.strptime(fecha_limpia, '%Y-%m-%d')

                                if inicio <= fecha_pedido <= fin:
                                    pedidos_filtrados.append(p)
                            except Exception as e:
                                print(f"⚠️ Error parseando fecha {fecha_pedido_str}: {e}")
                                continue

                    pedidos = pedidos_filtrados
                    print(f"📊 Pedidos filtrados: {len(pedidos)} para el período")

                except Exception as e:
                    print(f"❌ Error filtrando fechas en reporte completo: {e}")
                    import traceback
                    traceback.print_exc()
                    return {
                        'success': False,
                        'error': f'Error filtrando fechas: {str(e)}'
                    }

            if not pedidos:
                return {
                    'success': True,
                    'timestamp': datetime.now().isoformat(),
                    'resumen': {
                        'total_productos': len(productos),
                        'total_usuarios': len(usuarios),
                        'total_pedidos': 0,
                        'ingresos_totales': 0,
                        'ingresos_pagados': 0,
                        'ingresos_entregados': 0,
                        'pedidos_pagados': 0,
                        'pedidos_entregados': 0
                    },
                    'estados': {},
                    'top_productos_cantidad': [],
                    'top_productos_monto': [],
                    'ventas_categoria': {},
                    'top_clientes': [],
                    'ventas_por_dia': {},
                    'metodos_pago': {},
                    'tipos_entrega': {}
                }

            # Crear mapa de nombre -> categoría
            producto_categoria_map = {}
            for p in productos:
                nombre = p.get('nombre')
                categoria = p.get('tipo', 'Otros')
                if nombre and categoria:
                    if not categoria or categoria == 'Sin categoría':
                        categoria = 'Otros'
                    producto_categoria_map[nombre] = categoria

            # ESTADÍSTICAS BÁSICAS
            total_productos = len(productos)
            total_usuarios = len(usuarios)
            total_pedidos = len(pedidos)

            # INGRESOS TOTALES
            ingresos_totales = sum(p.get('total', 0) for p in pedidos)

            # ESTADOS DE PEDIDOS
            estados = {}
            for p in pedidos:
                estado = p.get('estado', 'DESCONOCIDO')
                estados[estado] = estados.get(estado, 0) + 1

            # PEDIDOS PAGADOS VS ENTREGADOS
            pedidos_pagados = [p for p in pedidos if p.get('estado') == 'PAGADO']
            pedidos_entregados = [p for p in pedidos if p.get('estado') == 'ENTREGADO']
            ingresos_pagados = sum(p.get('total', 0) for p in pedidos_pagados)
            ingresos_entregados = sum(p.get('total', 0) for p in pedidos_entregados)

            # PRODUCTOS MÁS VENDIDOS
            ventas_productos = {}
            for p in pedidos:
                for item in p.get('items', []):
                    nombre = item.get('nombreProducto', 'Producto')
                    cantidad = item.get('cantidad', 0)
                    subtotal = item.get('subtotal', 0)
                    if nombre in ventas_productos:
                        ventas_productos[nombre]['cantidad'] += cantidad
                        ventas_productos[nombre]['monto'] += subtotal
                    else:
                        ventas_productos[nombre] = {
                            'cantidad': cantidad,
                            'monto': subtotal
                        }

            top_productos_cantidad = sorted(
                ventas_productos.items(),
                key=lambda x: x[1]['cantidad'],
                reverse=True
            )[:10]

            top_productos_monto = sorted(
                ventas_productos.items(),
                key=lambda x: x[1]['monto'],
                reverse=True
            )[:10]

            # VENTAS POR CATEGORÍA
            ventas_categoria = {}
            for p in pedidos:
                for item in p.get('items', []):
                    nombre = item.get('nombreProducto', 'Producto')
                    subtotal = item.get('subtotal', 0)
                    categoria = producto_categoria_map.get(nombre, 'Otros')
                    if not categoria or categoria == 'Sin categoría':
                        categoria = self._determinar_categoria_por_nombre(nombre)
                    ventas_categoria[categoria] = ventas_categoria.get(categoria, 0) + subtotal

            # CLIENTES FRECUENTES (INCLUYE PRESENCIALES)
            clientes = {}
            for p in pedidos:
                if p.get('usuario'):
                    usuario = p.get('usuario', {})
                    nombre = f"{usuario.get('nombres', '')} {usuario.get('apellidos', '')}".strip()
                    if not nombre:
                        nombre = usuario.get('username', 'Cliente')
                elif p.get('nombreCliente'):
                    nombre = p.get('nombreCliente', '').strip()
                else:
                    nombre = 'Cliente general'

                if nombre:
                    clientes[nombre] = clientes.get(nombre, 0) + 1

            top_clientes = sorted(clientes.items(), key=lambda x: x[1], reverse=True)[:10]

            # VENTAS POR DÍA
            hoy = datetime.now().date()
            ventas_por_dia = {}
            for i in range(6, -1, -1):
                fecha = hoy - timedelta(days=i)
                fecha_str = fecha.strftime("%d/%m")
                inicio_dia = datetime.combine(fecha, datetime.min.time())
                fin_dia = datetime.combine(fecha, datetime.max.time())
                total_dia = 0
                for p in pedidos:
                    fecha_pedido_str = p.get('fecha')
                    if fecha_pedido_str:
                        try:
                            if 'Z' in fecha_pedido_str:
                                fecha_pedido = datetime.fromisoformat(fecha_pedido_str.replace('Z', '+00:00'))
                            else:
                                fecha_pedido = datetime.fromisoformat(fecha_pedido_str)
                            if inicio_dia <= fecha_pedido <= fin_dia:
                                total_dia += p.get('total', 0)
                        except:
                            pass
                ventas_por_dia[fecha_str] = round(total_dia, 2)

            # MÉTODOS DE PAGO
            metodos_pago = {}
            for p in pedidos:
                metodo = p.get('metodoPago', 'No especificado')
                if not metodo or metodo == '':
                    metodo = 'No especificado'
                metodos_pago[metodo] = metodos_pago.get(metodo, 0) + 1

            # TIPOS DE ENTREGA
            tipos_entrega = {}
            for p in pedidos:
                tipo = p.get('tipoEntrega', 'No especificado')
                if not tipo or tipo == '':
                    tipo = 'No especificado'
                tipos_entrega[tipo] = tipos_entrega.get(tipo, 0) + 1

            return {
                'success': True,
                'timestamp': datetime.now().isoformat(),
                'resumen': {
                    'total_productos': total_productos,
                    'total_usuarios': total_usuarios,
                    'total_pedidos': total_pedidos,
                    'ingresos_totales': round(ingresos_totales, 2),
                    'ingresos_pagados': round(ingresos_pagados, 2),
                    'ingresos_entregados': round(ingresos_entregados, 2),
                    'pedidos_pagados': len(pedidos_pagados),
                    'pedidos_entregados': len(pedidos_entregados)
                },
                'estados': estados,
                'top_productos_cantidad': [
                    {'nombre': k, 'cantidad': v['cantidad'], 'monto': round(v['monto'], 2)}
                    for k, v in top_productos_cantidad
                ],
                'top_productos_monto': [
                    {'nombre': k, 'cantidad': v['cantidad'], 'monto': round(v['monto'], 2)}
                    for k, v in top_productos_monto
                ],
                'ventas_categoria': ventas_categoria,
                'top_clientes': [{'nombre': k, 'pedidos': v} for k, v in top_clientes],
                'ventas_por_dia': ventas_por_dia,
                'metodos_pago': metodos_pago,
                'tipos_entrega': tipos_entrega
            }

        except Exception as e:
            print(f"❌ Error en obtener_reporte_completo: {e}")
            import traceback
            traceback.print_exc()
            return {
                'success': False,
                'error': str(e)
            }