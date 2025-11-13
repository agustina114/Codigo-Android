package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;           // Manejo de sesión del usuario
import com.google.firebase.auth.FirebaseUser;          // Datos del usuario actual
import com.google.firebase.firestore.FirebaseFirestore; // Base de datos Firestore
import com.google.firebase.firestore.QueryDocumentSnapshot; // Foto de documentos devueltos en una Query

/*
 * =====================================================
 * PANEL DEL COMPRADOR
 * =====================================================
 * Muestra las métricas principales para el usuario tipo "Comprador":
 *   ✔ Número de proveedores
 *   ✔ Número de productos activos
 *   ✔ Compras realizadas
 *   ✔ Total gastado
 *
 * También permite navegar a:
 *   - Proveedores
 *   - Productos
 *   - Carrito
 *
 * Esta Activity extiende BaseActivity → barra de estado transparente
 */
public class Panel_comprador extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private ImageButton btnLogout, btnNotifications, btnMenu;

    // TextViews del encabezado y estadísticas
    private TextView tvUserName, tvWelcome, tvProveedoresCount, tvProductosCount, tvComprasCount, tvTotalGastado;

    // Firebase
    private FirebaseAuth auth;            // Control de login/logout
    private FirebaseFirestore db;         // Acceso a Firestore
    private FirebaseUser user;            // Usuario actual logeado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel_comprador);

        // =====================================================
        // 1. INICIALIZAR FIREBASE
        // =====================================================
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = auth.getCurrentUser();

        // =====================================================
        // 2. ENLAZAR VISTAS XML
        // =====================================================
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnMenu = findViewById(R.id.btnMenu);

        tvUserName = findViewById(R.id.tvUserName);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvProveedoresCount = findViewById(R.id.tvProveedoresCount);
        tvProductosCount = findViewById(R.id.tvProductosCount);
        tvComprasCount = findViewById(R.id.tvComprasCount);
        tvTotalGastado = findViewById(R.id.tvTotalGastado);

        // Marcar pestaña "Inicio" como activa
        bottomNavigationView.setSelectedItemId(R.id.nav_inicio);

        // Validar si hay usuario
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // =====================================================
        // 3. CARGAR DATOS DEL USUARIO Y SUS ESTADÍSTICAS
        // =====================================================
        cargarNombreUsuario();
        cargarEstadisticas();

        // =====================================================
        // 4. CERRAR SESIÓN
        // =====================================================
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(Panel_comprador.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // =====================================================
        // 5. NAVEGACIÓN INFERIOR
        // =====================================================
        bottomNavigationView.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                return true; // ya estás aquí
            } else if (id == R.id.nav_proveedores) {
                startActivity(new Intent(this, Proveedores.class));
                overridePendingTransition(0, 0); // animación cero para continuidad visual
                return true;
            } else if (id == R.id.nav_productos) {
                startActivity(new Intent(this, Productos.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_carrito) {
                startActivity(new Intent(this, MiCarrito.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });

        // =====================================================
        // 6. TARJETAS PRINCIPALES DE NAVEGACIÓN
        // =====================================================
        CardView cardProveedores = findViewById(R.id.cardProveedores);
        CardView cardProductos = findViewById(R.id.cardProductos);
        CardView cardNuevaCompra = findViewById(R.id.cardNuevaCompra);

        // Tarjeta: ver proveedores
        cardProveedores.setOnClickListener(v -> {
            startActivity(new Intent(Panel_comprador.this, Proveedores.class));
            overridePendingTransition(0, 0);
        });

        // Tarjeta: ver productos
        cardProductos.setOnClickListener(v -> {
            startActivity(new Intent(Panel_comprador.this, Productos.class));
            overridePendingTransition(0, 0);
        });

        // Tarjeta: ir a nueva compra (carrito)
        cardNuevaCompra.setOnClickListener(v -> {
            startActivity(new Intent(Panel_comprador.this, MiCarrito.class));
            overridePendingTransition(0, 0);
        });

    }


    // ==========================================================
    // 📌 1. CARGAR NOMBRE DEL USUARIO (desde colección "compradores")
    // ==========================================================
    private void cargarNombreUsuario() {

        /*
         * db.collection("compradores").document(uid).get()
         * Devuelve un DocumentSnapshot con los datos del comprador.
         */
        db.collection("compradores")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {
                        String nombre = document.getString("nombre");

                        if (nombre != null && !nombre.isEmpty()) {
                            tvUserName.setText(nombre);
                            tvWelcome.setText("Bienvenido, " + nombre);
                        } else {
                            tvUserName.setText("Usuario");
                        }
                    }
                })
                .addOnFailureListener(e ->
                        tvUserName.setText("Usuario")
                );
    }

    // ==========================================================
    // 📌 2. CARGAR ESTADÍSTICAS DEL COMPRADOR
    // ==========================================================
    private void cargarEstadisticas() {

        // -------------------------  
        // A) Total de proveedores  
        // -------------------------
        db.collection("proveedores")
                .get()
                .addOnSuccessListener(snapshot ->
                        tvProveedoresCount.setText(String.valueOf(snapshot.size()))
                )
                .addOnFailureListener(e ->
                        tvProveedoresCount.setText("0")
                );

        // ---------------------------  
        // B) Total de productos activos  
        // ---------------------------
        db.collection("productos")
                .whereEqualTo("estado", "activo") // solo productos activos
                .get()
                .addOnSuccessListener(snapshot ->
                        tvProductosCount.setText(String.valueOf(snapshot.size()))
                )
                .addOnFailureListener(e ->
                        tvProductosCount.setText("0")
                );

        // ---------------------------  
        // C) Total de compras y gasto total  
        // ---------------------------
        db.collection("ordenes")
                .whereEqualTo("compradorId", user.getUid()) // compras del usuario actual
                .get()
                .addOnSuccessListener(snapshot -> {

                    int cantidadCompras = snapshot.size();
                    double totalGastado = 0.0;

                    /*
                     * QueryDocumentSnapshot:
                     * Representa cada documento dentro del QuerySnapshot.
                     * Funciona igual que DocumentSnapshot, pero se garantiza que viene de un Query.
                     */
                    for (QueryDocumentSnapshot doc : snapshot) {

                        Object subtotalObj = doc.get("subtotal");

                        if (subtotalObj != null) {
                            try {
                                // Convertir subtotal a double aunque venga como Object
                                totalGastado += Double.parseDouble(subtotalObj.toString());
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    tvComprasCount.setText(String.valueOf(cantidadCompras));
                    tvTotalGastado.setText("$" + String.format("%.0f", totalGastado));

                })
                .addOnFailureListener(e -> {
                    tvComprasCount.setText("0");
                    tvTotalGastado.setText("$0");
                });
    }
}
