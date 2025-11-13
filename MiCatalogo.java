package com.proveenet.proveenet;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Firebase Firestore — base de datos noSQL en la nube con documentos y colecciones.
import com.google.firebase.firestore.DocumentSnapshot;   // Foto 1 documento
import com.google.firebase.firestore.FirebaseFirestore; // Acceso a Firestore
import com.google.firebase.firestore.Query;             // Consultas ordenadas
import com.google.firebase.firestore.QuerySnapshot;     // Foto varios documentos

import java.util.HashMap;
import java.util.Map;

public class MiCatalogo extends BaseActivity {

    // --- Firebase ---
    private FirebaseAuth auth;           // Maneja sesión del usuario
    private FirebaseFirestore db;        // Conexión a Firestore

    // --- Vistas ---
    private LinearLayout llProductos, llProductosRecientes;
    private Button btnAgregarProducto;
    private TextView tvProductosCount;
    private TextView tvNombreEmpresa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_catalogo);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Vistas XML
        llProductos = findViewById(R.id.llProductos);
        llProductosRecientes = findViewById(R.id.llProductosRecientes);
        btnAgregarProducto = findViewById(R.id.btnAgregarProducto);
        tvProductosCount = findViewById(R.id.tvProductosCount);
        tvNombreEmpresa = findViewById(R.id.tvNombreEmpresa);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "⚠️ Debes iniciar sesión primero", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 🔹 Cargar info del proveedor
        cargarDatosProveedor(user);

        // 🔹 Escucha en tiempo real
        escucharProductosEnTiempoReal();
        escucharProductosRecientes();

        // 🔹 Botón agregar producto
        btnAgregarProducto.setOnClickListener(v -> mostrarModalAgregarProducto());

        // 🔹 NAVBAR inferior
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_catalogo);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, DashboardProveedor.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            if (id == R.id.nav_catalogo) return true;

            if (id == R.id.nav_ordenes) {
                startActivity(new Intent(this, MisOrdenes.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
    }

    // ===============================================================
    // 📌 Cargar datos del proveedor (empresa)
    private void cargarDatosProveedor(FirebaseUser user) {

        // Aquí obtenemos UN documento → DocumentSnapshot
        db.collection("proveedores").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {

                    /*
                     * DocumentSnapshot:
                     * Es una "foto" del documento en Firestore en ese momento.
                     * Permite leer campos con .getString(), .getLong(), etc.
                     */

                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("empresa");
                        tvNombreEmpresa.setText(nombre != null ? nombre : "Empresa sin nombre");
                    } else {
                        tvNombreEmpresa.setText("Perfil no encontrado");
                    }
                })
                .addOnFailureListener(e -> {
                    tvNombreEmpresa.setText("Error al cargar");
                });
    }

    // ===============================================================
    // 📌 Modal para agregar producto
    private void mostrarModalAgregarProducto() {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.modal_agregar_producto);
        dialog.setCancelable(false);

        // Ajusta tamaño del diálogo
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Inputs
        EditText etCodigo = dialog.findViewById(R.id.etCodigo);
        EditText etNombre = dialog.findViewById(R.id.etNombre);
        EditText etCategoria = dialog.findViewById(R.id.etCategoria);
        EditText etDescripcion = dialog.findViewById(R.id.etDescripcion);
        EditText etPrecio = dialog.findViewById(R.id.etPrecio);
        EditText etStock = dialog.findViewById(R.id.etStock);

        // Botones
        Button btnGuardar = dialog.findViewById(R.id.btnGuardar);
        Button btnCancelar = dialog.findViewById(R.id.btnCancelar);

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        // GUARDAR PRODUCTO
        btnGuardar.setOnClickListener(v -> {

            FirebaseUser user = auth.getCurrentUser();
            if (user == null) return;

            String codigo = etCodigo.getText().toString().trim().toUpperCase();
            String nombre = etNombre.getText().toString().trim();
            String categoria = etCategoria.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();

            long precio;
            long stock;

            // Validación numérica
            try {
                precio = Long.parseLong(etPrecio.getText().toString());
                stock = Long.parseLong(etStock.getText().toString());
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Ingresa números válidos", Toast.LENGTH_SHORT).show();
                return;
            }

            // MAP
            // Un Map es como un JSON: clave → valor
            Map<String, Object> producto = new HashMap<>();
            producto.put("codigo", codigo);
            producto.put("nombre", nombre);
            producto.put("categoria", categoria);
            producto.put("descripcion", descripcion);
            producto.put("precio", precio);
            producto.put("stock", stock);
            producto.put("estado", "activo");
            producto.put("proveedorId", user.getUid());
            producto.put("proveedorNombre", tvNombreEmpresa.getText().toString());

            // 🔹 Verificar si el código ya existe
            db.collection("productos").document(codigo)
                    .get()
                    .addOnSuccessListener(existing -> {

                        /*
                         * existing → DocumentSnapshot
                         * Permite preguntar .exists() para saber si ya hay un producto igual.
                         */

                        if (existing.exists()) {
                            Toast.makeText(this, "⚠️ Ese código ya existe", Toast.LENGTH_SHORT).show();
                        } else {

                            // Guardar producto
                            db.collection("productos").document(codigo)
                                    .set(producto)  // .set() crea o reemplaza
                                    .addOnSuccessListener(x -> {
                                        Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    });
                        }
                    });
        });

        dialog.show();
    }

    // ===============================================================
    // 📌 Escucha en tiempo real los productos del proveedor
    private void escucharProductosEnTiempoReal() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        /*
         * addSnapshotListener():
         * Escucha cambios en TIEMPO REAL.
         * QuerySnapshot:
         * Representa VARIOS documentos (como un array de DocumentSnapshots).
         */
        db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid())
                .addSnapshotListener((snapshots, e) -> {

                    if (e != null || snapshots == null) {
                        return;
                    }

                    // Limpiar UI
                    llProductos.removeAllViews();
                    int count = 0;

                    for (DocumentSnapshot doc : snapshots) {
                        // Inflate → Convierte un XML en una vista lista para usar.
                        View card = LayoutInflater.from(this)
                                .inflate(R.layout.item_producto_card, llProductos, false);

                        llenarCardProducto(doc, card);
                        llProductos.addView(card);
                        count++;
                    }

                    tvProductosCount.setText(
                            count == 0 ? "No hay productos" :
                            count == 1 ? "1 producto" :
                            count + " productos"
                    );
                });
    }

    // ===============================================================
    // 📌 Productos recientes ordenados por código
    private void escucharProductosRecientes() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        /*
         * Query + orderBy
         *
         * Firestore permite ordenar documentos por un campo.
         * orderBy("codigo", DESC) → últimos productos agregados.
         *
         * limit(2) → solo trae los 2 primeros.
         */
        db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid())
                .orderBy("codigo", Query.Direction.DESCENDING)
                .limit(2)
                .addSnapshotListener((snapshots, e) -> {

                    if (snapshots == null) return;

                    llProductosRecientes.removeAllViews();

                    for (DocumentSnapshot doc : snapshots) {
                        View card = LayoutInflater.from(this)
                                .inflate(R.layout.item_producto_card, llProductosRecientes, false);
                        llenarCardProducto(doc, card);
                        llProductosRecientes.addView(card);
                    }
                });
    }

    // ===============================================================
    // 📌 Rellena una card de producto
    private void llenarCardProducto(DocumentSnapshot doc, View card) {

        // DocumentSnapshot permite leer el valor de cada campo
        ((TextView) card.findViewById(R.id.tvNombre)).setText(doc.getString("nombre"));
        ((TextView) card.findViewById(R.id.tvCodigo)).setText("Código: " + doc.getString("codigo"));

        ((TextView) card.findViewById(R.id.tvCategoria)).setText("Categoría: " + doc.getString("categoria"));
        ((TextView) card.findViewById(R.id.tvDescripcion)).setText(doc.getString("descripcion"));
        ((TextView) card.findViewById(R.id.tvPrecio)).setText("$" + documentToString(doc.get("precio")));
        ((TextView) card.findViewById(R.id.tvStock)).setText(documentToString(doc.get("stock")) + " unidades");

        String estado = doc.getString("estado");
        TextView tvEstado = card.findViewById(R.id.tvEstado);

        if ("activo".equalsIgnoreCase(estado)) {
            tvEstado.setText("Estado: Activo");
            tvEstado.setTextColor(Color.parseColor("#2E7D32"));
            tvEstado.setBackgroundResource(R.drawable.badge_estado_activo);
        } else {
            tvEstado.setText("Estado: Inactivo");
            tvEstado.setTextColor(Color.parseColor("#757575"));
            tvEstado.setBackgroundResource(R.drawable.badge_estado_inactivo);
        }

        // --- Eliminar producto ---
        card.findViewById(R.id.btnEliminar).setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("Eliminar producto")
                    .setMessage("¿Seguro que deseas eliminar este producto?")
                    .setPositiveButton("Sí", (d, w) -> {

                        // delete() elimina un documento por ID
                        db.collection("productos").document(doc.getId())
                                .delete()
                                .addOnSuccessListener(x -> {
                                    llProductos.removeView(card);
                                });

                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // --- Editar producto ---
        card.findViewById(R.id.btnEditar).setOnClickListener(v -> {
            mostrarModalEditarProducto(doc);
        });
    }

    private String documentToString(Object obj) {
        return (obj instanceof Number)
                ? String.valueOf(((Number) obj).longValue())
                : (obj != null ? obj.toString() : "0");
    }

    // ===============================================================
    // 📌 Modal para editar producto existente
    private void mostrarModalEditarProducto(DocumentSnapshot doc) {

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.modal_editar_producto);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etNombreEdit = dialog.findViewById(R.id.etNombreEdit);
        EditText etCategoriaEdit = dialog.findViewById(R.id.etCategoriaEdit);
        EditText etDescripcionEdit = dialog.findViewById(R.id.etDescripcionEdit);
        EditText etPrecioEdit = dialog.findViewById(R.id.etPrecioEdit);
        EditText etStockEdit = dialog.findViewById(R.id.etStockEdit);

        Button btnGuardarEdit = dialog.findViewById(R.id.btnGuardarEdit);
        Button btnCancelarEdit = dialog.findViewById(R.id.btnCancelarEdit);

        // Prellenar con valores del DocumentSnapshot
        etNombreEdit.setText(doc.getString("nombre"));
        etCategoriaEdit.setText(doc.getString("categoria"));
        etDescripcionEdit.setText(doc.getString("descripcion"));
        etPrecioEdit.setText(String.valueOf(doc.get("precio")));
        etStockEdit.setText(String.valueOf(doc.get("stock")));

        btnCancelarEdit.setOnClickListener(v -> dialog.dismiss());

        btnGuardarEdit.setOnClickListener(v -> {

            long precio, stock;

            try {
                precio = Long.parseLong(etPrecioEdit.getText().toString());
                stock = Long.parseLong(etStockEdit.getText().toString());
            } catch (Exception e) {
                Toast.makeText(this, "Ingrese valores válidos", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> actualizaciones = new HashMap<>();
            actualizaciones.put("nombre", etNombreEdit.getText().toString());
            actualizaciones.put("categoria", etCategoriaEdit.getText().toString());
            actualizaciones.put("descripcion", etDescripcionEdit.getText().toString());
            actualizaciones.put("precio", precio);
            actualizaciones.put("stock", stock);

            // update() actualiza SOLO los campos enviados
            db.collection("productos").document(doc.getId())
                    .update(actualizaciones)
                    .addOnSuccessListener(x -> dialog.dismiss());
        });

        dialog.show();
    }
}
