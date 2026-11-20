/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
import { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, Search } from 'lucide-react';
import apiClient from '../services/api';
import { Cliente } from '../types';
import Button from '../components/Button';
import Input from '../components/Input';
import Modal from '../components/Modal';

const Clientes = () => {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCliente, setEditingCliente] = useState<Cliente | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [negotiationFilter, setNegotiationFilter] = useState('TODAS');
  const [portfolioFilter, setPortfolioFilter] = useState('TODOS');
  const [commercialTypeFilter, setCommercialTypeFilter] = useState('TODOS');
  const [commercialOwnerFilter, setCommercialOwnerFilter] = useState('TODOS');
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState<Cliente>({
    nombre: '',
    apellido: '',
    email: '',
    telefono: '',
    celular: '',
    direccion: '',
    ciudad: '',
    pais: '',
    codigoPostal: '',
    identificacion: '',
    tipoIdentificacion: '',
    empresa: '',
    cargo: '',
    sector: '',
    notas: '',
    estado: 'ACTIVO',
    estadoNegociacion: 'SIN_NEGOCIACION',
    tieneCartera: false,
    tipoGestionComercial: 'SIN_CLASIFICAR',
    responsableComercial: '',
  });
  
  useEffect(() => {
    loadClientes();
  }, []);
  
  const loadClientes = async () => {
    try {
      const response = await apiClient.get('/api/clientes');
      setClientes(response.data);
    } catch (err) {
      console.error('Error loading clientes:', err);
    }
  };
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      console.log('Enviando datos:', formData);
      if (editingCliente?.id) {
        await apiClient.put(`/api/clientes/${editingCliente.id}`, formData);
      } else {
        await apiClient.post('/api/clientes', formData);
      }
      loadClientes();
      setIsModalOpen(false);
      setEditingCliente(null);
      setFormData({
        nombre: '',
        apellido: '',
        email: '',
        telefono: '',
        celular: '',
        direccion: '',
        ciudad: '',
        pais: '',
        codigoPostal: '',
        identificacion: '',
        tipoIdentificacion: '',
        empresa: '',
        cargo: '',
        sector: '',
        notas: '',
        estado: 'ACTIVO',
        estadoNegociacion: 'SIN_NEGOCIACION',
        tieneCartera: false,
        tipoGestionComercial: 'SIN_CLASIFICAR',
        responsableComercial: '',
      });
    } catch (error: any) {
      console.error('Error saving cliente:', error);
      console.error('Error response:', error.response);
      console.error('Error data:', error.response?.data);
      const errorMessage = error.response?.data || error.message || 'Error al guardar el cliente';
      setError(typeof errorMessage === 'string' ? errorMessage : JSON.stringify(errorMessage));
    }
  };
  
  const handleEdit = (cliente: Cliente) => {
    setEditingCliente(cliente);
    setFormData(cliente);
    setError(null);
    setIsModalOpen(true);
  };
  
  const handleDelete = async (id: number) => {
    if (window.confirm('¿Está seguro de eliminar este cliente?')) {
      try {
        await apiClient.delete(`/api/clientes/${id}`);
        loadClientes();
      } catch (error) {
        console.error('Error deleting cliente:', error);
      }
    }
  };
  
  const filteredClientes = clientes.filter((cliente) => {
    const query = searchTerm.trim().toLowerCase();
    const matchesSearch = !query ||
      cliente.nombre.toLowerCase().includes(query) ||
      cliente.apellido.toLowerCase().includes(query) ||
      cliente.email?.toLowerCase().includes(query) ||
      cliente.empresa?.toLowerCase().includes(query) ||
      cliente.responsableComercial?.toLowerCase().includes(query);
    const matchesNegotiation = negotiationFilter === 'TODAS' ||
      (cliente.estadoNegociacion || 'SIN_NEGOCIACION') === negotiationFilter;
    const matchesPortfolio = portfolioFilter === 'TODOS' ||
      Boolean(cliente.tieneCartera) === (portfolioFilter === 'SI');
    const matchesCommercialType = commercialTypeFilter === 'TODOS' ||
      (cliente.tipoGestionComercial || 'SIN_CLASIFICAR') === commercialTypeFilter;
    const matchesCommercialOwner = commercialOwnerFilter === 'TODOS' ||
      cliente.responsableComercial === commercialOwnerFilter;
    return matchesSearch && matchesNegotiation && matchesPortfolio && matchesCommercialType && matchesCommercialOwner;
  });

  const commercialOwners = Array.from(new Set(
    clientes.map((cliente) => cliente.responsableComercial).filter((value): value is string => Boolean(value))
  )).sort((a, b) => a.localeCompare(b, 'es'));

  const displayName = (cliente: Cliente) =>
    cliente.tipoIdentificacion === 'ZEUZ' && cliente.empresa
      ? cliente.empresa
      : `${cliente.nombre} ${cliente.apellido}`.trim();

  const negotiationLabel = (value?: Cliente['estadoNegociacion']) => ({
    SIN_NEGOCIACION: 'Sin negociación',
    EN_NEGOCIACION: 'En negociación',
    ACUERDO_ACTIVO: 'Acuerdo activo',
  }[value || 'SIN_NEGOCIACION']);

  const commercialTypeLabel = (value?: Cliente['tipoGestionComercial']) => ({
    AGENCIA: 'Agencia',
    FREELANCE: 'Freelance',
    SIN_CLASIFICAR: 'Sin clasificar',
  }[value || 'SIN_CLASIFICAR']);
  
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Clientes</h1>
        <Button onClick={() => { setIsModalOpen(true); setError(null); }}>
          <Plus className="w-4 h-4 mr-2" />
          Nuevo Cliente
        </Button>
      </div>
      
      <div className="card mb-6 grid grid-cols-1 gap-3 lg:grid-cols-5">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
          <Input
            placeholder="Buscar por nombre o email..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10"
          />
        </div>
        <select className="input" value={commercialTypeFilter} onChange={(e) => setCommercialTypeFilter(e.target.value)}>
          <option value="TODOS">Agencias y freelance</option>
          <option value="AGENCIA">Agencias</option>
          <option value="FREELANCE">Freelance</option>
          <option value="SIN_CLASIFICAR">Sin clasificar</option>
        </select>
        <select className="input" value={commercialOwnerFilter} onChange={(e) => setCommercialOwnerFilter(e.target.value)}>
          <option value="TODOS">Todos los responsables</option>
          {commercialOwners.map((owner) => <option key={owner} value={owner}>{owner}</option>)}
        </select>
        <select className="input" value={negotiationFilter} onChange={(e) => setNegotiationFilter(e.target.value)}>
          <option value="TODAS">Todas las negociaciones</option>
          <option value="SIN_NEGOCIACION">Sin negociación</option>
          <option value="EN_NEGOCIACION">En negociación</option>
          <option value="ACUERDO_ACTIVO">Acuerdo activo</option>
        </select>
        <select className="input" value={portfolioFilter} onChange={(e) => setPortfolioFilter(e.target.value)}>
          <option value="TODOS">Toda la cartera</option>
          <option value="SI">Tiene cartera</option>
          <option value="NO">Sin cartera</option>
        </select>
      </div>
      
      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Tipo comercial</th>
              <th>Agencia / freelance</th>
              <th>Email</th>
              <th>Negociación</th>
              <th>Tiene cartera</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filteredClientes.map((cliente) => (
              <tr key={cliente.id}>
                <td>
                  {displayName(cliente)}
                </td>
                <td>
                  <span className={`rounded-full px-2 py-1 text-xs font-medium ${cliente.tipoGestionComercial === 'AGENCIA' ? 'bg-sky-100 text-sky-800' : cliente.tipoGestionComercial === 'FREELANCE' ? 'bg-violet-100 text-violet-800' : 'bg-gray-100 text-gray-600'}`}>
                    {commercialTypeLabel(cliente.tipoGestionComercial)}
                  </span>
                </td>
                <td>{cliente.responsableComercial || <span className="text-gray-400">Sin asignar</span>}</td>
                <td>{cliente.email || <span className="text-gray-400">Sin email</span>}</td>
                <td>
                  <span className="rounded-full bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700">
                    {negotiationLabel(cliente.estadoNegociacion)}
                  </span>
                </td>
                <td>
                  <span className={`rounded-full px-2 py-1 text-xs font-medium ${cliente.tieneCartera ? 'bg-amber-100 text-amber-800' : 'bg-gray-100 text-gray-600'}`}>
                    {cliente.tieneCartera ? 'Sí' : 'No'}
                  </span>
                </td>
                <td>
                  <div className="flex space-x-2">
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleEdit(cliente)}
                    >
                      <Edit className="w-4 h-4" />
                    </Button>
                    <Button
                      variant="danger"
                      size="sm"
                      onClick={() => handleDelete(cliente.id!)}
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {filteredClientes.length === 0 && (
          <p className="text-center text-gray-500 py-8">No hay clientes registrados</p>
        )}
      </div>
      
      <Modal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingCliente(null);
          setError(null);
        }}
        title={editingCliente?.id ? 'Editar Cliente' : 'Nuevo Cliente'}
      >
        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
            {error}
          </div>
        )}
        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Nombre"
              value={formData.nombre}
              onChange={(e) => setFormData({ ...formData, nombre: e.target.value })}
              required
            />
            <Input
              label="Apellido"
              value={formData.apellido}
              onChange={(e) => setFormData({ ...formData, apellido: e.target.value })}
              required
            />
            <Input
              label="Email"
              type="email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            />
            <Input
              label="Teléfono"
              value={formData.telefono}
              onChange={(e) => setFormData({ ...formData, telefono: e.target.value })}
            />
            <Input
              label="Celular"
              value={formData.celular}
              onChange={(e) => setFormData({ ...formData, celular: e.target.value })}
            />
            <Input
              label="Empresa"
              value={formData.empresa}
              onChange={(e) => setFormData({ ...formData, empresa: e.target.value })}
            />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Tipo comercial</label>
              <select
                className="input"
                value={formData.tipoGestionComercial || 'SIN_CLASIFICAR'}
                onChange={(e) => setFormData({ ...formData, tipoGestionComercial: e.target.value as Cliente['tipoGestionComercial'] })}
              >
                <option value="AGENCIA">Agencia</option>
                <option value="FREELANCE">Freelance</option>
                <option value="SIN_CLASIFICAR">Sin clasificar</option>
              </select>
            </div>
            <Input
              label="Agencia / freelance responsable"
              value={formData.responsableComercial || ''}
              onChange={(e) => setFormData({ ...formData, responsableComercial: e.target.value })}
            />
            <Input
              label="Cargo"
              value={formData.cargo}
              onChange={(e) => setFormData({ ...formData, cargo: e.target.value })}
            />
            <Input
              label="Identificación"
              value={formData.identificacion}
              onChange={(e) => setFormData({ ...formData, identificacion: e.target.value })}
            />
            <Input
              label="Ciudad"
              value={formData.ciudad}
              onChange={(e) => setFormData({ ...formData, ciudad: e.target.value })}
            />
            <Input
              label="País"
              value={formData.pais}
              onChange={(e) => setFormData({ ...formData, pais: e.target.value })}
            />
          </div>
          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">Notas</label>
            <textarea
              className="input"
              rows={3}
              value={formData.notas}
              onChange={(e) => setFormData({ ...formData, notas: e.target.value })}
            />
          </div>
          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">Negociación</label>
            <select
              className="input"
              value={formData.estadoNegociacion || 'SIN_NEGOCIACION'}
              onChange={(e) => setFormData({ ...formData, estadoNegociacion: e.target.value as Cliente['estadoNegociacion'] })}
            >
              <option value="SIN_NEGOCIACION">Sin negociación</option>
              <option value="EN_NEGOCIACION">En negociación</option>
              <option value="ACUERDO_ACTIVO">Acuerdo activo</option>
            </select>
          </div>
          <label className="mt-4 flex items-center gap-2 text-sm font-medium text-gray-700">
            <input
              type="checkbox"
              checked={Boolean(formData.tieneCartera)}
              onChange={(e) => setFormData({ ...formData, tieneCartera: e.target.checked })}
            />
            Tiene cartera
          </label>
          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">Estado</label>
            <select
              className="input"
              value={formData.estado}
              onChange={(e) => setFormData({ ...formData, estado: e.target.value as any })}
            >
              <option value="ACTIVO">Activo</option>
              <option value="INACTIVO">Inactivo</option>
              <option value="BLOQUEADO">Bloqueado</option>
            </select>
          </div>
          <div className="mt-6 flex justify-end space-x-3">
            <Button
              type="button"
              variant="secondary"
              onClick={() => {
                setIsModalOpen(false);
                setEditingCliente(null);
              }}
            >
              Cancelar
            </Button>
            <Button type="submit">Guardar</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default Clientes;
