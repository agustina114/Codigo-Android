package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

// Firebase para autenticación y base de datos
import com.google.firebase.auth.FirebaseAuth;        // Maneja login/logout
import com.google.firebase.auth.FirebaseUser;       // Usuario activo actual
import com.google.firebase.firestore.DocumentSnapshot; // Foto de 1 documento
import com.google.firebase.firestore.FirebaseFirestore; // Base Firestore NoSQL

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MisOrdenes extends BaseActivity {

    // RecyclerView = lista dinámica optimizada
    private RecyclerView recyclerOrdenes;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Adapter = puente entre Firestore y RecyclerView
    private OrdenAdapter ordenAdapter;

    // Lista donde se guardan las órdenes en memoria antes de mostrarlas
    private List<Map<String, Object>> listaOrdenes;

    // Header
    private TextView tvNombreEmpresa, tvOrdenesCount;
    private ImageButton btnMenu, btnLogout, btnNotifications;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_ordenes);

        // ==========================================================
        // 1. INICIALIZAR FIREBASE
        // ==========================================================
        db = FirebaseFirestore.getInstance();  // Acceso a Firestore
        auth = FirebaseAuth.getInstance();     // Manejo del login actual

        // ==========================================================
        // 2. CONFIGURAR RECYCLER VIEW
        // ==========================================================
        recyclerOrdenes = findViewById(R.id.recyclerOrdenes);

        /*
         * LinearLayoutManager:
         * Define que la lista se muestre verticalmente como un ScrollView.
         */
        recyclerOrdenes.setLayoutManager(new LinearLayoutManager(this));

        listaOrdenes = new ArrayList<>();

        /*
         * OrdenAdapter:
         * El Adapter recibe la lista y la base de datos.
         * Su función es construir cada "tarjeta" de orden dentro del RecyclerView.
         */
        ordenAdapter = new OrdenAdapter(listaOrdenes, db);
        recyclerOrdenes.setAdapter(ordenAdapter);

        // ==========================================================
        // 3. VISTAS DEL HEADER Y BARRA INFERIOR
        // ==========================================================
        tvNombreEmpresa = findViewById(R.id.tvNombreEmpresa);
        tvOrdenesCount = findViewById(R.id.tvOrdenesCount);
        btnMenu = findViewById(R.id.btnMenu);
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setSelectedItemId(R.id.nav_ordenes);

        // ==========================================================
        // 4. VALIDAR SESIÓN ACTIVA
        // ==========================================================
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "⚠️ Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // ==========================================================
        // 5. CARGAR DATOS INICIALES
        // ==========================================================
        cargarNombreProveedor();     // Nombre del proveedor en el header
        cargarOrdenesProveedor();    // Cargar lista de órdenes

        // ==========================================================
        // 6. EVENTOS DE BOTONES
        // ==========================================================
        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(MisOrdenes.this, DashboardProveedor.class));
            overridePendingTransition(0, 0);
        });

        btnNotifications.setOnClickListener(v ->
                Toast.makeText(this, "🔔 Próximamente notificaciones", Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> {
            auth.signOut(); // Cerrar sesión

            Intent i = new Intent(MisOrdenes.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        // ==========================================================
        // 7. BOTTOM NAVIGATION
        // ==========================================================
        bottomNavigationView.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, DashboardProveedor.class));
                overridePendingTransition(0, 0);
                return true;
            }
            if (id == R.id.nav_catalogo) {
                startActivity(new Intent(this, MiCatalogo.class));
                overridePendingTransition(0, 0);
                return true;
            }
            if (id == R.id.nav_ordenes) {
                return true; // Ya estamos en esta pantalla
            }

            return false;
        });
    }

    // ==========================================================
    // 📌 CARGAR NOMBRE DEL PROVEEDOR (HEADER)
    // ==========================================================
    private void cargarNombreProveedor() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        /*
         * db.collection("proveedores").document(uid).get():
         * Obtiene UN documento → DocumentSnapshot
         *
         * DocumentSnapshot:
         *   Es una foto de los datos del documento en ese instante.
         *   Permite leer valores con .getString("campo").
         */
        db.collection("proveedores").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombreEmpresa = doc.getString("nombreEmpresa");
                        tvNombreEmpresa.setText(
                                nombreEmpresa != null ? nombreEmpresa : "Proveedor"
                        );
                    } else {
                        tvNombreEmpresa.setText("Proveedor");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Error al cargar nombre: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    // ==========================================================
    // 📌 CARGAR LISTA DE ÓRDENES DEL PROVEEDOR
    // ==========================================================
    private void cargarOrdenesProveedor() {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        /*
         * .whereEqualTo("proveedorId", user.getUid())
         * Filtra las órdenes por proveedor.
         *
         * .get()
         * Obtiene una foto de TODOS los documentos → QuerySnapshot
         *
         * QuerySnapshot:
         *   Contiene una lista de DocumentSnapshot (documentos individuales).
         */
        db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {

                    listaOrdenes.clear();

                    // snapshot.isEmpty() → Si no hay órdenes
                    if (snapshot.isEmpty()) {
                        tvOrdenesCount.setText("No hay órdenes");
                        ordenAdapter.notifyDataSetChanged();
                        return;
                    }

                    /*
                     * snapshot.getDocuments():
                     * Devuelve una lista de DocumentSnapshot
                     */
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        /*
                         * doc.getData():
                         * Retorna un MAP (clave -> valor) con todos los campos del documento.
                         *
                         * Map<String, Object>:
                         * Estructura tipo JSON.
                         */
                        Map<String, Object> orden = doc.getData();

                        if (orden != null) {

                            orden.put("id", doc.getId()); // Guardar ID del documento

                            // Evitar errores por campos nulos
                            orden.putIfAbsent("productoNombre", "Producto desconocido");
                            orden.putIfAbsent("compradorNombre", "Comprador desconocido");
                            orden.putIfAbsent("subtotal", 0.0);

                            listaOrdenes.add(orden);
                        }
                    }

                    /*
                     * notifyDataSetChanged():
                     * Le indica al adapter que los datos cambiaron
                     * para que actualice el RecyclerView.
                     */
                    ordenAdapter.notifyDataSetChanged();

                    int total = listaOrdenes.size();
                    tvOrdenesCount.setText(total == 1 ? "1 orden" : total + " órdenes");
                })
                .addOnFailureListener(e -> {
                    tvOrdenesCount.setText("Error al cargar");
                    Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
