package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater; // ➜ Para convertir XML en Views
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;         // ➜ Maneja la sesión del usuario
import com.google.firebase.auth.FirebaseUser;        // ➜ Representa al usuario actual
import com.google.firebase.firestore.DocumentSnapshot; // ➜ Representa un documento de Firestore
import com.google.firebase.firestore.FirebaseFirestore; // ➜ Base de datos Firestore
import com.google.firebase.firestore.QuerySnapshot;     // ➜ Resultado de una consulta (varios documentos)

import java.util.HashMap;
import java.util.Map;

/*
 * ======================================================
 * ACTIVITY: Productos (vista del comprador)
 * ======================================================
 * Esta pantalla muestra todos los productos con estado "activo".
 * Los productos vienen desde Firestore → colección "productos".
 * Cada producto se muestra en una card inflada desde XML.
 * Permite agregar productos al carrito (colección anidada).
 */
public class Productos extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout llListaProductos; // contenedor de las cards del producto
    private TextView tvProductosCount, tvUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos);

        // ======================================================
        // 1. INICIALIZAR FIREBASE
        // ======================================================
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ======================================================
        // 2. ENLAZAR ELEMENTOS DEL LAYOUT
        // ======================================================
        llListaProductos = findViewById(R.id.llListaProductos); // LinearLayout donde se insertan cards dinámicas
        tvProductosCount = findViewById(R.id.tvProductosCount);
        tvUserName = findViewById(R.id.tvUserName);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setSelectedItemId(R.id.nav_productos);

        mostrarNombreUsuario();
        cargarProductosDisponibles();
        setupBottomNavigation();
    }


    // ======================================================
    // Muestra nombre del usuario (puede ser comprador O proveedor)
    // ======================================================
    private void mostrarNombreUsuario() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            tvUserName.setText("Usuario desconocido");
            return;
        }

        String uid = user.getUid();

        /*
         * Primero busca en "compradores".
         * DocumentSnapshot representa los datos del documento encontrado.
         */
        db.collection("compradores").document(uid).get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {
                        String nombre = document.getString("nombre");

                        if (nombre != null && !nombre.isEmpty()) {
                            tvUserName.setText(nombre);
                        } else {
                            tvUserName.setText("Sin nombre");
                        }

                    } else {
                        // No es comprador → buscar proveedor
                        db.collection("proveedores").document(uid).get()
                                .addOnSuccessListener(docProv -> {

                                    if (docProv.exists()) {
                                        String nombre = docProv.getString("nombre");
                                        tvUserName.setText(
                                                (nombre != null && !nombre.isEmpty()) ? nombre : "Sin nombre"
                                        );
                                    } else {
                                        tvUserName.setText("Usuario desconocido");
                                    }

                                })
                                .addOnFailureListener(e -> tvUserName.setText("Error al cargar nombre"));
                    }

                })
                .addOnFailureListener(e -> tvUserName.setText("Error al cargar nombre"));
    }


    // ======================================================
    // Configura barra inferior de navegación
    // ======================================================
    private void setupBottomNavigation() {

        /*
         * bottomNavigationView.setOnItemSelectedListener:
         * Detecta cuál botón de la barra inferior se presionó.
         */
        bottomNavigationView.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, Panel_comprador.class));
                finish();
                return true;
            }
            if (id == R.id.nav_proveedores) {
                startActivity(new Intent(this, Proveedores.class));
                finish();
                return true;
            }
            if (id == R.id.nav_productos) {
                return true; // estás aquí
            }
            if (id == R.id.nav_carrito) {
                startActivity(new Intent(this, MiCarrito.class));
                finish();
                return true;
            }

            return false;
        });
    }


    // ======================================================
    // Cargar productos activos desde Firestore
    // ======================================================
    private void cargarProductosDisponibles() {

        /*
         * whereEqualTo("estado", "activo"):
         * Filtra solo productos disponibles para los compradores.
         */
        db.collection("productos")
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(this::mostrarProductos)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al cargar productos", Toast.LENGTH_LONG).show()
                );
    }


    // ======================================================
    // Muestra los productos en tarjetas dinámicas
    // ======================================================
    private void mostrarProductos(QuerySnapshot snapshot) {

        /*
         * QuerySnapshot:
         * Representa un conjunto de DocumentSnapshot (lista de documentos).
         */
        llListaProductos.removeAllViews(); // limpiar antes de llenar
        int count = 0;

        for (DocumentSnapshot doc : snapshot.getDocuments()) {

            // =========== LEER DATOS DEL PRODUCTO DESDE FIRESTORE ===========
            String codigo = doc.getId();
            String nombre = doc.getString("nombre");
            String categoria = doc.getString("categoria");
            String descripcion = doc.getString("descripcion");
            String proveedor = doc.getString("proveedorNombre");
            String proveedorId = doc.getString("proveedorId");

            // --- precio puede ser Number o String → validar
            Object precioObj = doc.get("precio");
            double precio = 0.0;
            String precioTxt = "0";

            if (precioObj instanceof Number) {
                precio = ((Number) precioObj).doubleValue();
                precioTxt = String.format("%.0f", precio); // quitar decimales
            } else if (precioObj instanceof String) {
                try {
                    precio = Double.parseDouble(((String) precioObj).replaceAll("[^0-9.]", ""));
                    precioTxt = String.valueOf(precio);
                } catch (Exception ignored) {}
            }

            // --- stock (puede venir como texto o número)
            Object stockObj = doc.get("stock");
            String stockTxt = "0";

            if (stockObj instanceof Number) {
                stockTxt = stockObj.toString();
            } else if (stockObj instanceof String) {
                stockTxt = (String) stockObj;
            }

            // ======================================================
            // Crear card inflando layout XML item_producto_publico
            // ======================================================

            /*
             * LayoutInflater:
             * Convierte un archivo XML en un objeto View en tiempo real.
             */
            View cardView = LayoutInflater.from(this)
                    .inflate(R.layout.item_producto_publico, llListaProductos, false);

            // Asignar datos a la card
            ((TextView) cardView.findViewById(R.id.tvNombreProducto)).setText(nombre != null ? nombre : "Sin nombre");
            ((TextView) cardView.findViewById(R.id.tvPrecio)).setText("$" + precioTxt);
            ((TextView) cardView.findViewById(R.id.tvProveedor)).setText(proveedor != null ? proveedor : "Desconocido");
            ((TextView) cardView.findViewById(R.id.tvStock)).setText(stockTxt + " disponibles");
            ((TextView) cardView.findViewById(R.id.tvCategoria)).setText(categoria != null ? categoria : "Sin categoría");
            ((TextView) cardView.findViewById(R.id.tvDescripcion)).setText(descripcion != null ? descripcion : "Sin descripción");

            // Botón agregar
            Button btnAgregar = cardView.findViewById(R.id.btnAgregarCotizacion);

            // Variables finales para lambdas
            double finalPrecio = precio;
            String finalProveedorId = proveedorId;

            // Acción del botón
            btnAgregar.setOnClickListener(v ->
                    agregarAlCarrito(codigo, nombre, proveedor, finalProveedorId, finalPrecio)
            );

            // Agregar card al LinearLayout
            llListaProductos.addView(cardView);

            count++;
        }

        tvProductosCount.setText(count + " disponibles");
    }


    // ======================================================
    // Agregar producto al carrito del usuario
    // ======================================================
    private void agregarAlCarrito(String productoId, String nombre, String proveedor,
                                  String proveedorId, double precio) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "⚠️ Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        /*
         * Estructura del carrito:
         *
         * carritos → userId → items → productoId
         */
        db.collection("carritos")
                .document(userId)
                .collection("items")
                .document(productoId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {

                        // Si ya existe → aumentar la cantidad
                        Long cantidadActual = doc.getLong("cantidad");
                        if (cantidadActual == null) cantidadActual = 1L;

                        db.collection("carritos")
                                .document(userId)
                                .collection("items")
                                .document(productoId)
                                .update("cantidad", cantidadActual + 1)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this, "🛒 Cantidad actualizada", Toast.LENGTH_SHORT).show()
                                );

                    } else {

                        // Crear nuevo ítem en el carrito
                        Map<String, Object> item = new HashMap<>();
                        item.put("nombre", nombre);
                        item.put("proveedor", proveedor);
                        item.put("proveedorId", proveedorId);
                        item.put("precio", precio);
                        item.put("cantidad", 1L);
                        item.put("productoId", productoId);

                        db.collection("carritos")
                                .document(userId)
                                .collection("items")
                                .document(productoId)
                                .set(item)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this, "✅ Agregado al carrito: " + nombre, Toast.LENGTH_SHORT).show()
                                );
                    }
                });
    }
}
