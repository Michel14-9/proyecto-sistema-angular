// src/app/modules/admin/admin-usuarios/admin-usuarios.ts
import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsersService, Usuario } from '../../../core/services/users.service';
import { AlertService } from '../../../core/services/alert.service';
import { ConfirmModalService } from '../../../core/services/confirm-modal.service';

declare var bootstrap: any;

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-usuarios.html',
  styleUrls: ['./admin-usuarios.css']
})
export class AdminUsuariosComponent implements OnInit, AfterViewInit, OnDestroy {
  usuarios: Usuario[] = [];
  usuariosFiltrados: Usuario[] = [];
  busquedaUsuarios: string = '';
  filtroRol: string = '';
  usuarioForm: Usuario = {
    id: 0,
    nombres: '',
    apellidos: '',
    tipoDocumento: '',
    numeroDocumento: '',
    telefono: '',
    fechaNacimiento: '',
    username: '',
    rol: '',
    password: ''
  };
  usuarioEditando: boolean = false;
  guardandoUsuario: boolean = false;
  modalUsuarioTitulo: string = 'Agregar Usuario';
  private userModal: any = null;

  constructor(
    private usersService: UsersService,
    private alertService: AlertService,
    private confirmModal: ConfirmModalService
  ) {}

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  ngAfterViewInit(): void {
    const userModalEl = document.getElementById('userModal');
    if (userModalEl) {
      this.userModal = new bootstrap.Modal(userModalEl);
    }
  }

  ngOnDestroy(): void {
    if (this.userModal) {
      this.userModal.dispose();
    }
  }

  cargarUsuarios(): void {
    this.usersService.getAll().subscribe({
      next: (data: any) => {
        if (Array.isArray(data)) {
          this.usuarios = data.filter((u: any) => u.activo !== false);
          this.filtrarUsuarios();
        }
      },
      error: () => this.alertService.mostrar('Error al cargar usuarios', 'warning')
    });
  }

  filtrarUsuarios(): void {
    let filtrados = this.usuarios;
    if (this.busquedaUsuarios.trim()) {
      const term = this.busquedaUsuarios.toLowerCase().trim();
      filtrados = filtrados.filter(u =>
        u.nombres.toLowerCase().includes(term) ||
        u.apellidos.toLowerCase().includes(term) ||
        u.username.toLowerCase().includes(term)
      );
    }
    if (this.filtroRol) {
      filtrados = filtrados.filter(u => u.rol.toLowerCase() === this.filtroRol.toLowerCase());
    }
    this.usuariosFiltrados = filtrados;
  }

  abrirModalUsuario(usuario?: Usuario): void {
    this.usuarioEditando = !!usuario;
    this.modalUsuarioTitulo = usuario ? 'Editar Usuario' : 'Agregar Usuario';
    this.usuarioForm = usuario ? { ...usuario, password: '' } : {
      id: 0,
      nombres: '',
      apellidos: '',
      tipoDocumento: '',
      numeroDocumento: '',
      telefono: '',
      fechaNacimiento: '',
      username: '',
      rol: '',
      password: ''
    };
    if (this.userModal) this.userModal.show();
  }

  cerrarModalUsuario(): void {
    if (this.userModal) this.userModal.hide();
  }

  guardarUsuario(): void {
    const campos = [
      { val: this.usuarioForm.nombres, msg: 'Los nombres son requeridos' },
      { val: this.usuarioForm.apellidos, msg: 'Los apellidos son requeridos' },
      { val: this.usuarioForm.tipoDocumento, msg: 'Debe seleccionar un tipo de documento' },
      { val: this.usuarioForm.numeroDocumento, msg: 'El número de documento es requerido' },
      { val: this.usuarioForm.telefono, msg: 'El teléfono es requerido' },
      { val: this.usuarioForm.fechaNacimiento, msg: 'La fecha de nacimiento es requerida' },
      { val: this.usuarioForm.username, msg: 'El email es requerido' },
      { val: this.usuarioForm.rol, msg: 'Debe seleccionar un rol' }
    ];
    for (const campo of campos) {
      if (!campo.val) {
        this.alertService.mostrar(campo.msg, 'warning');
        return;
      }
    }
    if (!this.usuarioForm.password || this.usuarioForm.password.length < 6) {
      this.alertService.mostrar('La contraseña debe tener al menos 6 caracteres', 'warning');
      return;
    }

    this.guardandoUsuario = true;
    const formData = new FormData();
    formData.append('nombres', this.usuarioForm.nombres);
    formData.append('apellidos', this.usuarioForm.apellidos);
    formData.append('tipoDocumento', this.usuarioForm.tipoDocumento);
    formData.append('numeroDocumento', this.usuarioForm.numeroDocumento);
    formData.append('telefono', this.usuarioForm.telefono);
    formData.append('fechaNacimiento', this.usuarioForm.fechaNacimiento);
    formData.append('email', this.usuarioForm.username);
    formData.append('rol', this.usuarioForm.rol);
    formData.append('password', this.usuarioForm.password);

    this.usersService.create(formData).subscribe({
      next: () => {
        this.guardandoUsuario = false;
        this.cerrarModalUsuario();
        this.alertService.mostrar('Usuario guardado', 'success');
        this.cargarUsuarios();
      },
      error: (error: any) => {
        this.guardandoUsuario = false;
        console.error(' Error guardando usuario:', error);
        this.alertService.mostrar('Error al guardar el usuario', 'danger');
      }
    });
  }

  confirmarEliminarUsuario(usuario: Usuario): void {
    this.confirmModal.confirmar(
      'Confirmar Eliminación',
      `¿Eliminar a "${usuario.nombres} ${usuario.apellidos}"?`,
      () => this.eliminarUsuario(usuario.id)
    );
  }

  eliminarUsuario(id: number): void {
    this.usersService.delete(id).subscribe({
      next: (response: any) => {
        this.alertService.mostrar(response.desactivado ? 'Usuario desactivado' : 'Usuario eliminado', 'success');
        this.cargarUsuarios();
      },
      error: (error: any) => {
        console.error(' Error eliminando usuario:', error);
        this.alertService.mostrar('Error al eliminar el usuario', 'danger');
      }
    });
  }

  getRolBadgeClass(rol: string): string {
    const clases: { [key: string]: string } = {
      'admin': 'bg-danger',
      'cajero': 'bg-warning',
      'cocinero': 'bg-info',
      'delivery': 'bg-primary'
    };
    return clases[rol?.toLowerCase()] || 'bg-secondary';
  }
}
