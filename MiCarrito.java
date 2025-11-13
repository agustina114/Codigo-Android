package com.proveenet.proveenet;

import androidx.appcompat.app.AlertDialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class MiCarrito extends BaseActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // --- Vistas ---
    private LinearLayout llCarrito, llCarritoVacio, llResumen;
    private ScrollView scrollCarrito;
    private TextView tvTotalProductos, tvSubtotal, tvIva, tvTotal;
    private ImageButton btnBack, btnVaciarCarrito;
    private Button btnFinalizarCompra, btnExplorarProductos;
    private BottomNavigationView bottomNavigationView;

    // --- Variables ---
    private double subtotalGlobal = 0.0;

    // 🔹 itemsActuales → Lista en memoria con los productos del carrito
    // Cada producto se almacena como un MAP (clave → valor)
    // Ejemplo: {"nombre": "Manzanas", "cantidad": 3, "precio": 1000}
    private List<Map<String, Object>> itemsActuales = new ArrayList<>();

    // 🔹 carritoListener → escucha en TIEMPO REAL cambios al carrito
    private ListenerRegistration carritoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_carrito);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- Obtener vistas del XML ---
        llCarrito = findViewById(R.id.llCarrito);
        llCarritoVacio = findViewById(R.id.llCarritoVacio);
        llResumen = findViewById(R.id.llResumen);
        scrollCarrito = findViewById(R.id.scrollCarrito);

        tvTotalProductos = findViewById(R.id.tvTotalProductos);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvIva = findViewById(R.id.tvIva);
        tvTotal = findViewById(R.id.tvTotal);

        btnFinalizarCompra = findViewById(R.id.btnCotizar);
        btnVaciarCarrito = findViewById(R.id.btnVaciarCarrito);
        btnExplorarProductos = findViewById(R.id.btnExplorarProductos);
        btnBack = findViewById(R.id.btnBack);

        bottomNavigationView = findViewById(R.id.bottomNavigation);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "⚠️ Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bottomNavigationView.setSelectedItemId(R.id.nav_carrito);

        // 🔹 Cargar el carrito en tiempo real
        cargarCarrito();

        btnBack.setOnClickListener(v -> finish());
        btnExplorarProductos.setOnClickListener(v -> {
            startActivity(new Intent(this, Productos.class));
            finish();
        });

        btnVaciarCarrito.setOnClickListener(v -> vaciarCarrito());
        btnFinalizarCompra.setOnClickListener(v -> confirmarFinalizacion());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, Panel_comprador.class));
                finish();
                return true;
            } else if (id == R.id.nav_productos) {
                startActivity(new Intent(this, Productos.class));
                finish();
                return true;
            } else if (id == R.id.nav_proveedores) {
                startActivity(new Intent(this, Proveedores.class));
                finish();
                return true;
            } else if (id == R.id.nav_carrito) {
                return true;
            }
            return false;
        });
    }

    // ============================================================
    // 🔹 SNAPSHOT LISTENER
    // addSnapshotListener() escucha cambios en tiempo real.
    // Si el usuario agrega o quita productos desde otro dispositivo,
    // la UI se actualiza automáticamente.
    private void cargarCarrito() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        carritoListener = db.collection("carritos")
                .document(user.getUid())
                .collection("items")
                .addSnapshotListener((snapshots, e) -> {

                    // snapshots → QuerySnapshot
                    // Es una "foto" (snapshot) de los datos en ese momento
                    if (e != null) {
                        Toast.makeText(this, "Error al cargar carrito", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        mostrarCarritoVacio();
                        return;
                    }

                    mostrarCarritoConProductos(snapshots);
                });
    }

    // ============================================================
    private void mostrarCarritoConProductos(QuerySnapshot snapshots) {

        scrollCarrito.setVisibility(View.VISIBLE);
        llResumen.setVisibility(View.VISIBLE);
        llCarritoVacio.setVisibility(View.GONE);

        llCarrito.removeAllViews();
        itemsActuales.clear();
        subtotalGlobal = 0.0;

        int totalItems = 0;

        // 🔹 QuerySnapshot = lista de documentos
        // 🔹 DocumentSnapshot = un documento específico
        for (DocumentSnapshot doc : snapshots) {

            String nombre = doc.getString("nombre");
            String proveedor = doc.getString("proveedor");
            String productoId = doc.getId();
            String proveedorId = doc.getString("proveedorId");

            Double precio = doc.getDouble("precio");
            Long cantidad = doc.getLong("cantidad");

            if (precio == null)
                precio = 0.0;
            if (cantidad == null)
                cantidad = 1L;

            double subtotalItem = precio * cantidad;

            subtotalGlobal += subtotalItem;
            totalItems += cantidad;

            // 🔹 MAP
            // Un Map funciona como un JSON:
            // clave → valor
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("productoId", productoId);
            itemMap.put("nombre", nombre);
            itemMap.put("proveedor", proveedor);
            itemMap.put("proveedorId", proveedorId);
            itemMap.put("precio", precio);
            itemMap.put("cantidad", cantidad);

            itemsActuales.add(itemMap);

            // Inflar vista del producto
            View itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_carrito, llCarrito, false);

            ((TextView) itemView.findViewById(R.id.tvNombreCarrito)).setText(nombre);
            ((TextView) itemView.findViewById(R.id.tvProveedorCarrito)).setText(proveedor);
            ((TextView) itemView.findViewById(R.id.tvPrecioCarrito)).setText("$" + String.format("%.0f", precio));
            ((TextView) itemView.findViewById(R.id.tvCantidadCarrito)).setText(String.valueOf(cantidad));
            ((TextView) itemView.findViewById(R.id.tvSubtotalCarrito))
                    .setText("$" + String.format("%.0f", subtotalItem));

            Button btnMenos = itemView.findViewById(R.id.btnMenos);
            Button btnMas = itemView.findViewById(R.id.btnMas);
            Button btnEliminar = itemView.findViewById(R.id.btnEliminarCarrito);

            long cantidadActual = cantidad;

            btnMenos.setOnClickListener(v -> {
                if (cantidadActual > 1)
                    actualizarCantidad(productoId, cantidadActual - 1);
            });

            btnMas.setOnClickListener(v -> actualizarCantidad(productoId, cantidadActual + 1));

            btnEliminar.setOnClickListener(v -> eliminarDelCarrito(productoId));

            llCarrito.addView(itemView);
        }

        double iva = subtotalGlobal * 0.19;
        double totalFinal = subtotalGlobal + iva;

        tvTotalProductos.setText(totalItems + " productos");
        tvSubtotal.setText("$" + String.format("%.0f", subtotalGlobal));
        tvIva.setText("$" + String.format("%.0f", iva));
        tvTotal.setText("$" + String.format("%.0f", totalFinal));
    }

    // ============================================================
    private void actualizarCantidad(String productoId, long nuevaCantidad) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        db.collection("carritos")
                .document(user.getUid())
                .collection("items")
                .document(productoId)
                .update("cantidad", nuevaCantidad);
    }

    // ============================================================
    private void eliminarDelCarrito(String productoId) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        new AlertDialog.Builder(this)
                .setTitle("Eliminar producto")
                .setMessage("¿Deseas quitar este producto del carrito?")
                .setPositiveButton("Sí", (d, w) -> {

                    // 🔹 Eliminación simple
                    db.collection("carritos")
                            .document(user.getUid())
                            .collection("items")
                            .document(productoId)
                            .delete();

                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ============================================================
    private void vaciarCarrito() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        new AlertDialog.Builder(this)
                .setTitle("Vaciar carrito")
                .setMessage("¿Seguro que deseas vaciar todo el carrito?")
                .setPositiveButton("Sí", (d, w) -> {

                    // 🔹 BATCH = varias operaciones juntas
                    // WriteBatch permite borrar muchos documentos en un solo request
                    WriteBatch batch = db.batch();

                    db.collection("carritos")
                            .document(user.getUid())
                            .collection("items")
                            .get()
                            .addOnSuccessListener(snapshots -> {

                                // snapshots = QuerySnapshot
                                for (DocumentSnapshot doc : snapshots) {
                                    // doc.getReference() → referencia al documento
                                    batch.delete(doc.getReference());
                                }

                                batch.commit(); // Ejecuta TODAS las operaciones juntas
                            });

                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ============================================================
    private void confirmarFinalizacion() {
        if (itemsActuales.isEmpty()) {
            Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Finalizar compra")
                .setMessage("¿Deseas confirmar la compra y generar la orden?")
                .setPositiveButton("Sí", (dialog, which) -> crearOrden())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ============================================================
    private void crearOrden() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        String compradorId = user.getUid();

        // 🔹 Nombre del comprador (email si no tiene nombre)
        String compradorNombre = user.getDisplayName() != null ? user.getDisplayName()
                : (user.getEmail() != null ? user.getEmail() : "Usuario Anónimo");

        // 🔹 BATCH → para crear muchas órdenes juntas
        WriteBatch batch = db.batch();

        for (Map<String, Object> item : itemsActuales) {

            // Extracción desde el MAP
            String productoId = (String) item.get("productoId");
            String productoNombre = (String) item.get("nombre");
            String proveedorNombre = (String) item.get("proveedor");
            String proveedorId = (String) item.get("proveedorId");
            double precioUnitario = (double) item.get("precio");
            long cantidad = (long) item.get("cantidad");
            double subtotal = precioUnitario * cantidad;

            // Crear una nueva orden como MAP
            Map<String, Object> orden = new HashMap<>();
            orden.put("compradorId", compradorId);
            orden.put("compradorNombre", compradorNombre);

            orden.put("productoId", productoId);
            orden.put("productoNombre", productoNombre);

            orden.put("proveedorNombre", proveedorNombre);
            orden.put("proveedorId", proveedorId);

            orden.put("cantidad", cantidad);
            orden.put("precioUnitario", precioUnitario);
            orden.put("subtotal", subtotal);

            // 🔹 serverTimestamp() → hora exacta del servidor
            orden.put("fechaCreacion", FieldValue.serverTimestamp());
            orden.put("estado", "pendiente");
            orden.put("confirmacionProveedor", "pendiente");

            // Genera ID automáticamente
            DocumentReference ordenRef = db.collection("ordenes").document();

            // Se agrega al batch la creación de cada orden
            batch.set(ordenRef, orden);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Orden generada correctamente", Toast.LENGTH_SHORT).show();
                    vaciarCarritoSilencioso();
                });
    }

    // ============================================================
    private void vaciarCarritoSilencioso() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return;

        WriteBatch batch = db.batch();

        db.collection("carritos")
                .document(user.getUid())
                .collection("items")
                .get()
                .addOnSuccessListener(snapshots -> {

                    for (DocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit();
                });
    }

    private void mostrarCarritoVacio() {
        scrollCarrito.setVisibility(View.GONE);
        llResumen.setVisibility(View.GONE);
        llCarritoVacio.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // 🔹 IMPORTANTE:
        // Remove del listener para evitar memoria fugada
        if (carritoListener != null)
            carritoListener.remove();
    }
}
