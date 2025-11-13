# 📘 ProveeNet – Plataforma Android para Compradores y Proveedores

ProveeNet es una aplicación Android desarrollada en Java que conecta **compradores** con **proveedores**.
Incluye registro por rol, catálogos dinámicos, carrito de compras, órdenes, gestión de stock, paneles personalizados y Firebase como backend.

---

## 🛠 Tecnologías utilizadas

-   **Java (Android Studio)**
-   **Firebase Authentication**
-   **Cloud Firestore**
-   **RecyclerView**
-   **LayoutInflater**
-   **Colecciones anidadas en Firebase**
-   **QuerySnapshot & DocumentSnapshot**

---

## 🧠 Arquitectura por Roles

La app maneja **dos tipos de usuarios**, diferenciados por su colección y su campo `rol`.

### 👤 Compradores

Guardados en:

```bash
/compradores/{uid}
Campos típicos:

JSON

{
  "nombre": "Juan Pérez",
  "correo": "juan@gmail.com",
  "empresa": "Retail SPA",
  "rol": "comprador"
}
🏭 Proveedores
Guardados en:

Bash

/proveedores/{uid}
Campos típicos:

JSON

{
  "empresa": "ProveedorX",
  "correo": "contacto@proveedorx.cl",
  "rubro": "Ferretería",
  "telefono": "912345678",
  "direccion": "Av. Siempre Viva 123",
  "rol": "proveedor"
}
🧩 Estructura de Firestore
Markdown

📁 compradores
📁 proveedores
📁 productos
📁 carritos
  └── userId
        └── items
              └── productoId
📁 ordenes
📦 Funcionalidades principales
✔ Registro con dos roles

Formulario diferente para cada tipo de usuario

Firestore + FirebaseAuth

✔ Catálogo dinámico

Cards generadas con LayoutInflater

Filtro por proveedor

Formato visual atractivo

✔ Carrito funcional

Colecciones anidadas

Aumento de cantidad

Subtotales automáticos

✔ Órdenes

Confirmación de proveedor

Descuento de stock

Formato de fecha personalizada

❓ Preguntas Clave (y respuestas) para defensa
Todas estas explicaciones están orientadas a lo que normalmente preguntan en defensas de proyectos Android + Firebase.

✔️ ¿Qué es QuerySnapshot?
Es un conjunto de documentos devuelto por una consulta a Firestore.

Ejemplo:

Java

db.collection("productos")
  .whereEqualTo("estado", "activo")
  .get()
  .addOnSuccessListener(QuerySnapshot snapshot -> {
      // snapshot contiene la lista de productos
  });
Permite:

Recorrer documentos con un for.

Obtener su cantidad (snapshot.size()).

Obtener cada documento como DocumentSnapshot.

✔️ ¿Qué es DocumentSnapshot?
Representa un solo documento individual dentro de Firestore.

Ejemplo:

Java

// 'doc' es un DocumentSnapshot
String nombre = doc.getString("nombre");
long precio = doc.getLong("precio");
String id = doc.getId(); // Obtiene el ID del documento
Funciones clave:

exists(): Comprueba si el documento existe.

getString(), getLong(), get(): Obtienen campos específicos.

getId(): Obtiene el ID único (ej: productoId).

getData(): Devuelve un Map completo con todos los campos.

✔️ ¿Qué es LayoutInflater?
Es una clase de Android que convierte un archivo XML de layout en un objeto View de Java. Se usa para crear vistas dinámicamente en tiempo de ejecución.

Ejemplo real del proyecto:

Java

View card = LayoutInflater.from(this)
        .inflate(R.layout.item_producto_publico, llListaProductos, false);
Se usa porque los productos del catálogo NO están en el layout inicial; se inflan (crean) y agregan uno por uno usando el XML item_producto_publico.xml.

✔️ ¿Qué hace llListaProductos.addView()?
Simplemente agrega la vista (creada con LayoutInflater) al contenedor padre (llListaProductos, que es un LinearLayout).

Java

llListaProductos.addView(card);
Esto permite que el catálogo muestre tantas cards como productos existan en la base de datos.

✔️ ¿Por qué se usa String.format()?
Para formatear valores, especialmente precios, asegurando un formato estándar y evitando decimales no deseados.

Java

// Si 'precio' es 12000.0 (un double)
tvPrecio.setText("$" + String.format("%.0f", precio));
// El resultado será: $12000
Ventajas:

Evita que se muestren precios como $12000.0.

Asegura consistencia visual.

Permite formatear según el país (CL, US, ES).

✔️ ¿Qué es .whereEqualTo("estado","activo")?
Es un filtro de consulta de Firestore. Le dice a la base de datos que devuelva solo documentos donde el campo estado sea exactamente igual al valor activo.

Java

db.collection("productos")
  .whereEqualTo("estado", "activo") // Solo trae productos activos
  .get()
Así, productos "inactivos" o "pausados" no se muestran al comprador.

✔️ ¿Cómo funciona el carrito con colecciones anidadas?
Firestore utiliza esta estructura para aislar el carrito de cada usuario:

Markdown

carritos (Colección)
  └── userId (Documento)
        └── items (Sub-colección)
              └── productoId (Documento)
Así, el carrito de usuario_A está totalmente separado del de usuario_B.

Ejemplo al agregar un producto:

Java

// Obtenemos el ID del usuario y del producto
String userId = mAuth.getCurrentUser().getUid();
String productoId = producto.getId();

// Creamos la ruta anidadada
db.collection("carritos")
  .document(userId)
  .collection("items")
  .document(productoId) // El ID del producto se usa como ID del documento
  .set(item); // 'item' es un Map o POJO con {nombre, precio, cantidad}
Beneficios:

Ordenado por usuario.

Permite manejar campos por ítem (cantidad, subtotal).

Escalable y fácil de consultar (db.collection("carritos").document(userId).collection("items").get()).

✔️ ¿Por qué el campo precio se maneja como Object?
Porque Firestore puede devolver números en dos formatos: Long (si es un entero, ej: 1000) o Double (si tiene decimales, ej: 1000.50). Si se guardó mal, incluso podría ser un String.

Para evitar que la app crashee, se usa Object y luego se comprueba el tipo:

Java

Object precioObj = doc.get("precio");
double precio = 0.0; // Valor por defecto

if (precioObj instanceof Number) {
    // Number es la clase padre de Long y Double
    precio = ((Number) precioObj).doubleValue();
}
Con esto evitas errores (ClassCastException) si Firestore devuelve un tipo inesperado.

✔️ ¿Cómo se diferencian compradores y proveedores?
Se usan tres mecanismos:

Colecciones distintas:

/compradores

/proveedores

Campo rol: Cada documento de usuario tiene un campo que lo identifica.

JSON

"rol": "comprador"
// o
"rol": "proveedor"
Verificación en Login: Cuando un usuario inicia sesión (con FirebaseAuth), se toma su UID y se busca en Firestore:

Primero, se busca en /compradores/{uid}.

Si existe y rol == "comprador", se le envía al Panel_comprador.

Si no existe, se busca en /proveedores/{uid}.

Si existe y rol == "proveedor", se le envía al DashboardProveedor.
