# Sistema de Inventario y Pedidos

Sistema de gestion de inventario, pedidos y proveedores para tiendas.  
Proyecto educativo — Fundamentos de Bases de Datos, 4to semestre.

## Stack

| Capa | Tecnologia |
|---|---|
| Backend | Spring Boot 3.2.5 (Java 21), Spring Security + JWT, Spring Data JPA |
| Frontend | React 18 + TypeScript, Vite, TailwindCSS |
| Base de datos | H2 (autoritativo, siempre activo), MariaDB 10.11 (mirror/sync) |
| Mensajeria | STOMP sobre WebSocket (notificaciones en tiempo real) |
| Infraestructura | Docker Compose (MariaDB, backend, frontend + Nginx) |

## Arquitectura de base de datos

- **H2** es la base **autoritativa** — siempre activa, la app lee/escribe aqui
- **MariaDB** es un **mirror** — al arrancar, si esta disponible en `localhost:3307`, se sincroniza automaticamente desde H2 (schema + datos)
- Si MariaDB no esta disponible, la app funciona normalmente solo con H2
- `ddl-auto: update` en H2 — las tablas se crean/actualizan automaticamente
- La sincronizacion copia las 19 tablas en orden respetando dependencias

## Diagrama de Base de Datos (ER)

```
┌──────────────────────────────────────────────────────────────────┐
│                         USUARIO                                  │
│  id, email, password, full_name, enabled, role, time_zone       │
└────────┬──────────┬──────────┬──────────────────────────────────┘
         │          │          │
    ┌────▼───┐ ┌───▼──────┐ ┌─▼──────────┐
    │ ADMIN  │ │  DUENO   │ │ PROVEEDOR   │
    │USUARIO │ │ TIENDA   │ │             │
    │        │ │store_name│ │company_name │
    │        │ │address   │ │contact_phone│
    │        │ │favorite_ │ │emergency_   │
    │        │ │supplier  │ │email        │
    │        │ │          │ │address      │
    └────────┘ └──┬───────┘ └┬──────┬─────┘
                  │          │      │
      ┌───────────┘          │      └──────────────┐
      ▼                      ▼                     ▼
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│INVENTARIO│   │  ORDEN   │   │ PRODUCTO │   │UNIDAD    │
│          │   │  COMPRA  │   │          │   │MEDIDA    │
│cantidad  │   │          │   │nombre    │   │          │
│stock_min │   │estado    │   │codigo    │   │nombre    │
│stock_max │   │es_auto   │   │precio    │   │abrev     │
│ult_act   │   │fechas    │   │stock_min │   └──────────┘
└──┬───────┘   └──┬───────┘   │stock_max │        ▲
   │              │           └──┬───┬───┘        │
   │  ┌───────────┘              │   │            │
   │  ▼                          │   └────────────┘
   │ ┌──────────────┐            │
   │ │DETALLE_ORDEN │            │
   │ │   COMPRA     │            │
   │ │cantidad      │            │
   │ │precio_unit   │            │
   │ └──────────────┘            │
   │                             │
   ▼                             ▼
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│  LOTE    │   │  VENTA   │   │ HISTORIAL│   │  TICKET  │
│cantidad  │   │          │   │  PRECIO  │   │          │
│reservada │   │fecha     │   │          │   │estado    │
│fecha_exp │   │total     │   │precio_ant│   │votos     │
│precio    │   └──┬───────┘   │precio_nue│   │negocia   │
└──┬───────┘      │           └──────────┘   └──┬───────┘
   │         ┌────▼───────┐                     │
   │         │DETALLE     │              ┌──────▼───────┐
   │         │  VENTA     │              │   MENSAJE    │
   │         │cantidad    │              │contenido     │
   │         │precio_unit │              │rol_remitente│
   │         └────────────┘              └──────────────┘
   │
   ▼
┌──────────┐   ┌───────────┐   ┌──────────┐   ┌──────────┐
│NOTIFICACION│ │SOLICITUD  │   │ REGISTRO │   │  COLA    │
│mensaje   │   │REGISTRO   │   │AUDITORIA │   │ CORREO   │
│leida     │   │estado     │   │accion    │   │asunto    │
│tipo      │   │datos      │   │entidad   │   │reintentos│
└──────────┘   └───────────┘   └──────────┘   └──────────┘
```

### Tablas (nombre en español)

| Tabla | Descripción |
|---|---|
| `usuario` | Usuarios base (herencia JOINED) |
| `admin_usuario` | Administradores (extiende usuario) |
| `dueno_tienda` | Dueños de tienda (extiende usuario) |
| `proveedor` | Proveedores (extiende usuario) |
| `unidad_medida` | Catálogo de unidades de medida |
| `producto` | Productos de proveedores |
| `lote` | Lotes/batches de producto (cantidad, precio, expiración) |
| `inventario` | Stock por tienda con umbrales min/max |
| `orden_compra` | Órdenes de compra (tienda → proveedor) |
| `detalle_orden_compra` | Líneas de cada orden |
| `ticket` | Tickets de disputa/negociación |
| `mensaje` | Mensajes del chat de tickets |
| `venta` | Ventas realizadas (POS) |
| `detalle_venta` | Líneas de cada venta |
| `notificacion` | Notificaciones en tiempo real |
| `solicitud_registro` | Solicitudes de registro pendientes |
| `registro_auditoria` | Log de acciones del sistema |
| `historial_precio` | Historial de cambios de precio |
| `cola_correo` | Correos pendientes de envío |

## Roles

| Rol | Acceso | Funciones |
|---|---|---|
| **ROLE_ADMIN** | `/admin` | Dashboard, aprobar/rechazar registros, tickets (historial + voto final), auditoria, backups |
| **ROLE_STORE** | `/store` | Dashboard, inventario (min/max propio), crear pedidos, recibir pedidos, tickets |
| **ROLE_SUPPLIER** | `/supplier` | Dashboard, productos, agregar lotes, responder pedidos, tickets |

## Sistema de Tickets

### Flujo
```
1. Tienda recibe pedido con discrepancia → abre ticket
   o Proveedor disputa un pedido → abre ticket

2. Ticket status: OPEN → ambas partes pueden votar
   │
   ├── Ambos votan ACEPTAR → RESOLVED (automatico)
   ├── Ambos votan CANCELAR → RESOLVED (automatico)
   ├── Votos diferentes → notifica al admin → admin vota → RESOLVED
   ├── Timeout 5 min (alguien no voto) → notifica al admin
   └── Cualquiera puede cancelar en cualquier momento

3. Negociacion de precio:
   │
   ├── Cualquier parte propone un nuevo precio
   ├── La otra parte acepta → RESOLVED (con nuevo precio)
   └── La otra parte rechaza → admin decide (solo 1 ronda)
```

### Estados
- `OPEN` — Recien creado, esperando votos
- `VOTING` — Al menos una parte ya voto
- `NEGOTIATING` — Hay una negociacion de precio en curso
- `RESOLVED` — Resuelto (por consenso, admin, negociacion o cancelacion)

### Admin
- Ve historial completo en `/admin/tickets`
- Recibe notificaciones cuando hay desacuerdo o timeout
- Su voto es la decision final

## Stock por tienda

- Cada tienda define su propio `minStock` y `maxStock` por producto en el inventario
- Cuando el stock baja del minimo, se envia notificacion al dueño
- El reorden automatico usa los umbrales de la tienda (si no estan definidos, usa los del proveedor)

## Ejecucion

### Desarrollo rapido (H2 local)

```powershell
# Terminal 1 — Backend
cd backend
mvn spring-boot:run

# Terminal 2 — Frontend
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173`

### Con MariaDB (opcional)

Si tienes MariaDB corriendo en `localhost:3307` (user `root`, password vacio), la app lo detecta y sincroniza automaticamente.

```powershell
# Iniciar MariaDB con Docker
docker run -d --name mariadb -p 3307:3306 -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_DATABASE=market mariadb:10.11

# Luego iniciar backend
cd backend
mvn spring-boot:run
```

### Cuentas de prueba

| Email | Password | Rol |
|---|---|---|
| `admin@tienda.com` | `123456` | Administrador |
| `tienda@tienda.com` | `123456` | Tienda |
| `proveedor@tienda.com` | `123456` | Proveedor |

### Stack completo con Docker

```powershell
copy .env.example .env
docker-compose up -d
```

Abre `http://localhost` (puerto 80, Nginx)

## Flujo de registro

1. Usuario se registra en `/register/store` o `/register/supplier`
2. La solicitud queda en estado `PENDING` en la tabla `registration_request`
3. Un admin debe aprobarla manualmente desde `/admin/registrations`
4. Al aprobar, se crea el usuario real en la tabla `users`
5. El usuario ya puede iniciar sesion

**Nota:** No se puede iniciar sesion hasta que el admin apruebe el registro.

## Flujo de pedido

1. Tienda crea un pedido desde `/store/new-order`
2. Proveedor recibe notificacion y responde (acepta/rechaza) en `/supplier/orders`
3. Si acepta, la tienda confirma recepcion en `/store/orders`
4. Si hay discrepancia, se abre un ticket de resolucion
5. El ticket permite votacion entre tienda y proveedor (5 min)
6. Si no hay consenso, el admin tiene la decision final
7. Cualquier parte puede cancelar el ticket en cualquier momento

## Estructura del proyecto

```
shopFundDb/
├── backend/
│   ├── src/main/java/com/tienda/
│   │   ├── config/        # SecurityConfig, WebSocketConfig, DataInitializer, MariaDbSync, etc.
│   │   ├── controller/    # REST controllers (Auth, Admin, Store, Supplier)
│   │   ├── dto/           # Data Transfer Objects
│   │   ├── entity/        # JPA entities
│   │   ├── repository/    # Spring Data repositories
│   │   ├── security/      # JWT, Auth filter, UserDetailsService
│   │   └── service/       # Business logic
│   └── src/main/resources/
│       ├── application.yml          # Config base (H2 autoritativo)
│       ├── application-dev.yml      # Perfil desarrollo (H2 console)
│       ├── application-prod.yml     # Perfil produccion (MariaDB sync config)
│       └── db/migration/            # Migraciones Flyway
├── frontend/
│   └── src/
│       ├── components/    # UI components (Button, Input, Modal, etc.)
│       │   └── layout/    # AppShell, Sidebar, Header
│       ├── contexts/      # AuthContext, ThemeContext
│       ├── hooks/         # useNotifications
│       ├── pages/         # Login, Register, admin/, store/, supplier/
│       ├── services/      # api.ts (Axios), socket.ts (STOMP)
│       └── types/         # TypeScript interfaces
├── docker-compose.yml
└── README.md
```

## API Endpoints principales

### Auth (publico)
| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/auth/login` | Iniciar sesion |
| POST | `/api/auth/refresh` | Renovar token |
| POST | `/api/register` | Solicitar registro |

### Admin
| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | `/api/admin/registrations` | Solicitudes pendientes |
| POST | `/api/admin/registrations/{id}/approve` | Aprobar solicitud |
| POST | `/api/admin/registrations/{id}/reject` | Rechazar solicitud |
| GET | `/api/admin/tickets` | Tickets activos (?status=OPEN,VOTING,RESOLVED) |
| GET | `/api/admin/tickets/history` | Historial completo |
| POST | `/api/admin/tickets/{id}/vote` | Voto del admin (decision final) |
| POST | `/api/admin/tickets/{id}/cancel` | Cancelar ticket |
| GET | `/api/admin/audit` | Auditoria |

### Tienda
| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | `/api/store/inventory` | Ver inventario |
| PUT | `/api/store/inventory/{id}/thresholds` | Actualizar min/max stock |
| POST | `/api/store/orders` | Crear pedido |
| POST | `/api/store/orders/{id}/receive` | Confirmar recepcion |
| POST | `/api/store/orders/{id}/dispute` | Abrir ticket |
| GET | `/api/store/tickets` | Mis tickets |
| POST | `/api/store/tickets/{id}/vote` | Votar (ACCEPT/CANCEL) |
| POST | `/api/store/tickets/{id}/cancel` | Cancelar ticket |
| POST | `/api/store/tickets/{id}/propose-price` | Proponer precio |
| POST | `/api/store/tickets/{id}/negotiation-response` | Responder negociacion |

### Proveedor
| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | `/api/supplier/products` | Mis productos |
| POST | `/api/supplier/products` | Crear producto |
| GET | `/api/supplier/orders` | Pedidos recibidos |
| POST | `/api/supplier/orders/{id}/respond` | Responder pedido |
| POST | `/api/supplier/orders/{id}/dispute` | Abrir ticket |
| GET | `/api/supplier/tickets` | Mis tickets |
| POST | `/api/supplier/tickets/{id}/vote` | Votar |
| POST | `/api/supplier/tickets/{id}/cancel` | Cancelar ticket |
| POST | `/api/supplier/tickets/{id}/propose-price` | Proponer precio |
| POST | `/api/supplier/tickets/{id}/negotiation-response` | Responder negociacion |

## Variables de entorno

Copiar `.env.example` a `.env` y configurar:

| Variable | Descripcion | Default |
|---|---|---|
| `DB_HOST` | Host de MariaDB | `localhost` |
| `DB_PORT` | Puerto de MariaDB | `3307` |
| `DB_NAME` | Nombre de la base de datos | `market` |
| `DB_USER` | Usuario de BD | `root` |
| `DB_PASS` | Contrasena de BD | (vacio) |
| `JWT_SECRET` | Clave secreta JWT (min 32 chars) | `cambia-esta-clave-jwt-32-caracteres-minimo` |
