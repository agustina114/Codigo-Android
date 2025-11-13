package com.proveenet.proveenet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;   // ➜ Para convertir XML en Views dinámicas
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;   // ➜ Contenedor de las cards generadas
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;          // ➜ Maneja sesiones
import com.google.firebase.auth.FirebaseUser;         // ➜ Representa al usuario actual
import com.google.firebase.firestore.DocumentSnapshot; // ➜ Documento individual de Firestore
import com.google.firebase.firestore.FirebaseFirestore; // ➜ Base de datos Firestore
import com.google.firebase.firestore.QuerySnapshot;     // ➜ Resultado completo de una query

/*
 * =========================================================================
 * ACTIVITY: Proveedores (vista para compradores)
 * =========================================================================
 * Muestra una lista de proveedores que tienen el campo "rol: proveedor".
 * Cada proveedor se carga dinámicamente en una card.
 * La data proviene de Firestore → colección "proveedores".
 * =========================================================================
 */
public class Proveedores extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private LinearLayout llListaProveedores; // ➜ Contenedor donde se agregan las cards
    private TextView tvProveedoresCount, tvUserName;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proveedores);

        // ======================================================
        // 1. Inicializar Firebase
        // ======================================================
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ======================================================
        // 2. Vincular vistas XML
        // ======================================================
        llListaProveedores = findViewById(R.id.llListaProveedores);
        tvProveedoresCount = findViewById(R.id.tvProveedoresCount);
        tvUserName = findViewById(R.id.tvUserName);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setSelectedItemId(R.id.nav_proveedores);

        mostrarNombreUsuario();
        cargarProveedores();

        // ======================================================
        // 3. Navegación inferior
        // ======================================================
        bottomNavigationView.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, Panel_comprador.class));
                overridePendingTransition(0,0);
                return true;
            }

            if (id == R.id.nav_proveedores) {
                return true; // ya estamos aquí
            }

            if (id == R.id.nav_productos) {
                startActivity(new Intent(this, Productos.class));
                overridePendingTransition(0,0);
                return true;
            }

            return false;
        });
    }

    // ======================================================
    // 4. Mostrar nombre del usuario (puede ser comprador o proveedor)
    // ======================================================
    private void mostrarNombreUsuario() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            tvUserName.setText("Usuario desconocido");
            return;
        }

        String uid = user.getUid();

        /*
         * Primero se busca si es comprador.
         * DocumentSnapshot contiene TODOS los campos del documento.
         */
        db.collection("compradores").document(uid).get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {
                        String nombre = document.getString("nombre");
                        tvUserName.setText(
                                nombre != null && !nombre.isEmpty() ? nombre : "Sin nombre"
                        );
                    } else {
                        // Si no es comprador → buscar proveedor
                        db.collection("proveedores").document(uid).get()
                                .addOnSuccessListener(docProv -> {

                                    if (docProv.exists()) {
                                        String nombre = docProv.getString("nombre");
                                        tvUserName.setText(
                                                nombre != null && !nombre.isEmpty() ? nombre : "Sin nombre"
                                        );
                                    } else {
                                        tvUserName.setText("Usuario desconocido");
                                    }
                                })
                                .addOnFailureListener(e ->
                                        tvUserName.setText("Error al cargar nombre")
                                );
                    }

                })
                .addOnFailureListener(e ->
                        tvUserName.setText("Error al cargar nombre")
                );
    }


    // ======================================================
    // 5. Cargar proveedores que tengan rol = "proveedor"
    // ======================================================
    private void cargarProveedores() {

        /*
         * whereEqualTo("rol", "proveedor"):
         * Solo trae los documentos cuyo campo "rol" es exactamente "proveedor".
         *
         * QuerySnapshot = conjunto de documentos resultado de la consulta.
         */
        db.collection("proveedores")
                .whereEqualTo("rol", "proveedor")
                .get()
                .addOnSuccessListener(this::mostrarProveedores)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }


    // ======================================================
    // 6. Mostrar proveedores dentro del LinearLayout dinámico
    // ======================================================
    private void mostrarProveedores(QuerySnapshot snapshot) {

        llListaProveedores.removeAllViews(); // limpio la lista antes de llenarla
        int count = 0;

        /*
         * snapshot.getDocuments():
         * Devuelve una lista de DocumentSnapshot.
         */
        for (DocumentSnapshot doc : snapshot.getDocuments()) {

            // ======== LEER DATOS DEL PROVEEDOR ========
            String empresa = doc.getString("empresa");
            String rubro = doc.getString("rubro");
            String correo = doc.getString("correo");
            String telefono = doc.getString("telefono");
            String direccion = doc.getString("direccion");

            // ======================================================
            // Crear card con LayoutInflater
            // ======================================================

            /*
             * LayoutInflater.inflate:
             * Convierte el XML item_proveedor en un View real.
             */
            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_proveedor, llListaProveedores, false);

            // Vincular vistas internas
            TextView tvNombre = card.findViewById(R.id.tvNombreProveedor);
            TextView tvCategoria = card.findViewById(R.id.tvCategoria);
            TextView tvVerificado = card.findViewById(R.id.tvVerificado);
            TextView tvDireccion = card.findViewById(R.id.tvDireccion);
            TextView tvTelefono = card.findViewById(R.id.tvTelefono);

            Button btnCatalogo = card.findViewById(R.id.btnVerCatalogo);
            Button btnContactar = card.findViewById(R.id.btnContactar);

            // ======== ASIGNAR DATOS A LA CARD ========
            tvNombre.setText(empresa != null ? empresa : "Proveedor sin nombre");
            tvCategoria.setText(rubro != null ? rubro : "Sin categoría");

            // Campo que no existe → se esconde
            tvVerificado.setVisibility(View.GONE);

            tvDireccion.setText(
                    direccion != null && !direccion.isEmpty() ? direccion : "Sin dirección"
            );

            tvTelefono.setText(
                    telefono != null ? telefono : "Sin teléfono"
            );

            // ======== BOTONES DE ACCIÓN ========
            btnCatalogo.setOnClickListener(v ->
                    Toast.makeText(this, "📦 Ver catálogo de " + empresa, Toast.LENGTH_SHORT).show()
            );

            btnContactar.setOnClickListener(v ->
                    Toast.makeText(this, "📞 Contactar a " + empresa, Toast.LENGTH_SHORT).show()
            );

            // Agregar la card al LinearLayout
            llListaProveedores.addView(card);
            count++;
        }

        tvProveedoresCount.setText(count + " proveedores disponibles");
    }

}
