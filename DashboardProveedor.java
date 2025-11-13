package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DashboardProveedor extends BaseActivity {

    // ✨ Objetos principales de Firebase
    private FirebaseAuth auth;           // Maneja la sesión del usuario (login/logout)
    private FirebaseFirestore db;        // Acceso a la base de datos Firestore

    // 🔹 Referencias a la UI (TextViews, BottomNavigation, botón logout)
    private TextView tvNombreEmpresa, tvWelcome, tvProductosActivos, tvStockBajo, tvOrdenesRecibidas, tvVentasMes;
    private BottomNavigationView bottomNavigationView;
    private ImageButton btnLogout;

    // 🔹 Card de la última orden
    private LinearLayout llUltimaOrden;   // Contenedor de la card
    private TextView tvOrdenNumero, tvOrdenFecha, tvOrdenTotal, tvOrdenEstado, tvNoOrdenes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_proveedor);

        // 🔹 Inicializa Firebase (Auth + Firestore)
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 🔹 Vincula elementos del layout con el código
        tvNombreEmpresa = findViewById(R.id.tvNombreEmpresa);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvProductosActivos = findViewById(R.id.tvProductosActivos);
        tvStockBajo = findViewById(R.id.tvStockBajo);
        tvOrdenesRecibidas = findViewById(R.id.tvOrdenesRecibidas);
        tvVentasMes = findViewById(R.id.tvVentasMes);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnLogout = findViewById(R.id.btnLogout);

        // 🔹 Elementos de la card de última orden
        llUltimaOrden = findViewById(R.id.llUltimaOrden);
        tvOrdenNumero = findViewById(R.id.tvOrdenNumero);
        tvOrdenFecha = findViewById(R.id.tvOrdenFecha);
        tvOrdenTotal = findViewById(R.id.tvOrdenTotal);
        tvOrdenEstado = findViewById(R.id.tvOrdenEstado);
        tvNoOrdenes = findViewById(R.id.tvNoOrdenes);

        // ✅ Marca "Inicio" como el ítem seleccionado en la barra inferior
        bottomNavigationView.setSelectedItemId(R.id.nav_inicio);

        // 🚀 Carga de datos desde Firebase para rellenar el dashboard
        cargarNombreProveedor();   // Muestra nombre/empresa del proveedor
        cargarProductosActivos();  // Cuenta productos activos del proveedor
        cargarStockBajo();         // Cuenta productos con stock bajo (<= 5)
        cargarOrdenesRecibidas();  // Cuenta órdenes totales recibidas
        cargarVentasMes();         // Suma total de ventas del mes actual
        cargarUltimaOrden();       // 🆕 Muestra la última orden en la card

        // 🔹 Navegación inferior: define qué pasa cuando se toca cada ícono
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Si se toca "Inicio" no se hace nada (ya estamos aquí)
            if (id == R.id.nav_inicio) return true;

            // Navegar a "Mi catálogo"
            if (id == R.id.nav_catalogo) {
                startActivity(new Intent(this, MiCatalogo.class));
                overridePendingTransition(0, 0);
                return true;
            }

            // Navegar a "Mis órdenes"
            if (id == R.id.nav_ordenes) {
                startActivity(new Intent(this, MisOrdenes.class));
                overridePendingTransition(0, 0);
                return true;
            }

            return false;
        });

        // 🚪 Botón cerrar sesión
        btnLogout.setOnClickListener(v -> {
            auth.signOut(); // Cierra sesión en FirebaseAuth
            Toast.makeText(this, "👋 Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();

            // Limpia el historial de actividades y vuelve al MainActivity
            Intent intent = new Intent(DashboardProveedor.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Cierra este Activity
        });
    }

    // ==========================================================
    // 🔹 Carga el nombre del proveedor (empresa/correo) en el header
    private void cargarNombreProveedor() {
        FirebaseUser user = auth.getCurrentUser(); // Usuario logueado actualmente
        if (user == null) {
            // Si no hay sesión, muestra mensajes genéricos
            tvNombreEmpresa.setText("Sesión no detectada");
            tvWelcome.setText("Bienvenido");
            Toast.makeText(this, "⚠️ No se detectó sesión activa", Toast.LENGTH_SHORT).show();
            return;
        }

        // Busca el documento del proveedor con el mismo UID del usuario
        db.collection("proveedores").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Obtiene campos del documento
                        String empresa = doc.getString("empresa");
                        String correo = doc.getString("correo");

                        // Prioridad: empresa > correo del doc > correo de FirebaseAuth
                        if (empresa != null && !empresa.isEmpty()) {
                            tvNombreEmpresa.setText(empresa);
                            tvWelcome.setText("Bienvenido, " + empresa);
                        } else if (correo != null && !correo.isEmpty()) {
                            tvNombreEmpresa.setText(correo);
                            tvWelcome.setText("Bienvenido, " + correo);
                        } else {
                            tvNombreEmpresa.setText(user.getEmail());
                            tvWelcome.setText("Bienvenido, " + user.getEmail());
                        }
                    } else {
                        // Si no hay documento del proveedor, usa el correo del usuario
                        tvNombreEmpresa.setText(user.getEmail());
                        tvWelcome.setText("Bienvenido, " + user.getEmail());
                    }
                })
                .addOnFailureListener(e -> {
                    // En caso de error, igual muestra el correo del usuario
                    tvNombreEmpresa.setText(user.getEmail());
                    tvWelcome.setText("Bienvenido, " + user.getEmail());
                    Toast.makeText(this, "❌ Error al cargar proveedor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ==========================================================
    // 🔹 Cuenta cuántos productos activos tiene este proveedor
    private void cargarProductosActivos() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid()) // Filtro por proveedor
                .whereEqualTo("estado", "activo")           // Solo productos activos
                .get()
                .addOnSuccessListener(snapshot ->
                        // snapshot.size() = cantidad de documentos que cumplen el filtro
                        tvProductosActivos.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al contar productos", Toast.LENGTH_SHORT).show());
    }

    // 🔹 Cuenta productos con stock bajo (<= 5)
    private void cargarStockBajo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("productos")
                .whereEqualTo("proveedorId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    int bajos = 0;

                    // Recorre todos los productos del proveedor
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Object stockObj = doc.get("stock"); // stock puede venir como Number o String

                        if (stockObj != null) {
                            try {
                                int stock;
                                if (stockObj instanceof Number) {
                                    // Si Firestore lo guarda como número (Long/Double), lo convierte a int
                                    stock = ((Number) stockObj).intValue();
                                } else {
                                    // Si vino como String, lo parsea
                                    stock = Integer.parseInt(stockObj.toString());
                                }

                                // Si el stock es <= 5, se considera "stock bajo"
                                if (stock <= 5) bajos++;
                            } catch (Exception ignored) {
                                // Si falla el parseo, simplemente ignora ese producto
                            }
                        }
                    }

                    // Muestra cuántos productos tienen stock bajo
                    tvStockBajo.setText(String.valueOf(bajos));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al contar stock bajo: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 🔹 Cuenta cuántas órdenes totales tiene este proveedor
    private void cargarOrdenesRecibidas() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid())
                .get()
                .addOnSuccessListener(snapshot ->
                        tvOrdenesRecibidas.setText(String.valueOf(snapshot.size())))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al cargar órdenes", Toast.LENGTH_SHORT).show());
    }

    // ==========================================================
    // 🔹 Calcula el total de ventas del mes actual
    private void cargarVentasMes() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Formato año-mes, ejemplo: "2025-11"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String mesActual = sdf.format(Calendar.getInstance().getTime());

        db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid())
                .whereEqualTo("estado", "confirmada") // Solo órdenes confirmadas
                .get()
                .addOnSuccessListener(snapshot -> {
                    double totalMes = 0.0;

                    // Recorre todas las órdenes confirmadas del proveedor
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Object fechaObj = doc.get("fechaCreacion"); // Puede ser String o Timestamp
                        Object subtotalObj = doc.get("subtotal");   // Monto de la orden

                        if (fechaObj != null && subtotalObj != null) {
                            String fecha = fechaObj.toString(); // Se pasa a String para buscar "yyyy-MM"

                            // Si el String de fecha contiene el mes actual, se considera parte del mes
                            if (fecha.contains(mesActual)) {
                                try {
                                    // Suma el subtotal a totalMes
                                    totalMes += Double.parseDouble(subtotalObj.toString());
                                } catch (NumberFormatException ignored) {
                                    // Si no se puede convertir a número, se ignora esa orden
                                }
                            }
                        }
                    }

                    // Muestra el total de ventas del mes sin decimales
                    tvVentasMes.setText("$" + String.format("%.0f", totalMes));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al calcular ventas", Toast.LENGTH_SHORT).show());
    }

    // 🔹 Carga la última orden para mostrarla en la card
    private void cargarUltimaOrden() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("ordenes")
                .whereEqualTo("proveedorId", user.getUid())
                // Ordena por ID de documento de forma descendente (el último creado suele quedar al final)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .limit(1) // Solo queremos 1 documento: la "última" orden
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        // Si no hay órdenes, se oculta la card y se muestra mensaje "No tienes órdenes"
                        llUltimaOrden.setVisibility(View.GONE);
                        tvNoOrdenes.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Obtiene el único documento (la última orden)
                    DocumentSnapshot doc = snapshot.getDocuments().get(0);

                    String idOrden = doc.getId();               // ID del documento
                    String estado = doc.getString("estado");    // Estado de la orden
                    double subtotal = doc.getDouble("subtotal") != null ? doc.getDouble("subtotal") : 0.0;

                    // Muestra un ID corto de la orden (primeros 6 caracteres para que no sea tan largo)
                    tvOrdenNumero.setText("Orden #" + idOrden.substring(0, 6));

                    // Muestra el total de la orden
                    tvOrdenTotal.setText("$" + String.format("%.0f", subtotal));

                    // Capitaliza el estado (ej: "pendiente" -> "Pendiente")
                    if (estado != null && !estado.isEmpty()) {
                        tvOrdenEstado.setText(estado.substring(0,1).toUpperCase() + estado.substring(1));
                    } else {
                        tvOrdenEstado.setText("Pendiente");
                    }

                    // De momento, la fecha se muestra como "Reciente"
                    // (cuando uses un Timestamp real, aquí lo formateas)
                    tvOrdenFecha.setText("Reciente");

                    // Muestra la card y oculta el texto de "no órdenes"
                    llUltimaOrden.setVisibility(View.VISIBLE);
                    tvNoOrdenes.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    // En caso de error, oculta la card de última orden
                    llUltimaOrden.setVisibility(View.GONE);
                    tvNoOrdenes.setVisibility(View.VISIBLE);
                });
    }

}
