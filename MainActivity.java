¿

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    // 🔹 Elementos de UI
    private Button btnComprador, btnProveedor, btnLogin;
    private TextView tvLoginTitle, registrate;
    private EditText etEmail, etPassword;
    private ImageView ivLogo;

    // 🔹 Firebase
    private FirebaseAuth auth;              // Maneja login/logout
    private FirebaseFirestore db;           // Acceso a Firestore

    // 🔹 Para saber qué tipo de usuario está seleccionando el login
    private boolean esComprador = true;     // Por defecto, está seleccionado "Comprador"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            inicializarFirebase();   // Crea instancias de FirebaseAuth y Firestore
            inicializarUI();         // Enlaza elementos del layout
            configurarEventos();     // Asigna eventos a los botones
            actualizarFormulario();   // Cambia los colores y textos según el rol seleccionado
        } catch (Exception e) {
            Toast.makeText(this, "Error al iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // =======================================================
    // 🔹 Inicializa objetos de Firebase
    private void inicializarFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // 🔹 Vincula los elementos de la UI con el layout
    private void inicializarUI() {
        btnComprador = findViewById(R.id.btnComprador);
        btnProveedor = findViewById(R.id.btnProveedor);
        btnLogin = findViewById(R.id.btnLogin);

        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        registrate = findViewById(R.id.registrate);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivLogo = findViewById(R.id.ivLogo);
    }

    // 🔹 Configura listeners (acciones al presionar botones)
    private void configurarEventos() {

        // Al seleccionar comprador
        btnComprador.setOnClickListener(v -> {
            esComprador = true;
            actualizarFormulario();  // Actualiza colores y textos
        });

        // Al seleccionar proveedor
        btnProveedor.setOnClickListener(v -> {
            esComprador = false;
            actualizarFormulario();
        });

        // Botón "Ingresar"
        btnLogin.setOnClickListener(v -> iniciarSesion());

        // Botón "Regístrate"
        registrate.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Registro.class);
            startActivity(intent);
        });
    }


    // =======================================================
    // 🔹 Cambia colores, textos y estilos según el tipo de usuario seleccionado
    private void actualizarFormulario() {

        int blanco = ContextCompat.getColor(this, android.R.color.white);
        int negro = ContextCompat.getColor(this, android.R.color.black);

        // Paleta personalizada
        ColorStateList moradoComprador = ColorStateList.valueOf(Color.parseColor("#155dfc"));
        ColorStateList verdeProveedor = ColorStateList.valueOf(Color.parseColor("#00a63e"));

        if (esComprador) {
            // ======== MODO COMPRADOR ========

            // Cambia estilo de botones (uno seleccionado y otro no)
            btnComprador.setBackgroundResource(R.drawable.btn_selector_selected);
            btnProveedor.setBackgroundResource(R.drawable.btn_selector_unselected);
            btnComprador.setTextColor(blanco);
            btnProveedor.setTextColor(negro);

            // Pinta el botón comprador morado
            ViewCompat.setBackgroundTintList(btnComprador, moradoComprador);

            // Cambia título y placeholder
            tvLoginTitle.setText("Iniciar Sesión - Comprador");
            etEmail.setHint("tu@email.cl");

            // Botón ingresar también morado
            btnLogin.setBackgroundResource(R.drawable.button_blue_bg);
            ViewCompat.setBackgroundTintList(btnLogin, moradoComprador);

        } else {
            // ======== MODO PROVEEDOR ========

            btnProveedor.setBackgroundResource(R.drawable.btn_selector_selected);
            btnComprador.setBackgroundResource(R.drawable.btn_selector_unselected);
            btnProveedor.setTextColor(blanco);
            btnComprador.setTextColor(negro);

            // Pinta el botón proveedor verde
            ViewCompat.setBackgroundTintList(btnProveedor, verdeProveedor);

            // Cambia textos para proveedor
            tvLoginTitle.setText("Iniciar Sesión - Proveedor");
            etEmail.setHint("proveedor@empresa.cl");

            // Botón ingresar también verde
            btnLogin.setBackgroundResource(R.drawable.button_blue_bg);
            ViewCompat.setBackgroundTintList(btnLogin, verdeProveedor);
        }
    }


    // =======================================================
    // 🔹 Ejecuta el login con FirebaseAuth
    private void iniciarSesion() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validación básica
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inicia sesión en Firebase
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    FirebaseUser user = result.getUser();

                    if (user != null) {
                        String uid = user.getUid();

                        // IMPORTANTE:
                        // Comprador → debe existir en coleccion "compradores"
                        // Proveedor → debe existir en coleccion "proveedores"
                        if (esComprador) {
                            verificarRolEnFirestore(uid, "compradores");
                        } else {
                            verificarRolEnFirestore(uid, "proveedores");
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }


    // =======================================================
    // 🔹 Verifica que el usuario exista en su colección correcta
    private void verificarRolEnFirestore(String uid, String coleccion) {

        db.collection(coleccion).document(uid).get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        // Si existe, redirige al panel correcto
                        if (coleccion.equals("compradores")) {
                            startActivity(new Intent(this, Panel_comprador.class));
                        } else {
                            startActivity(new Intent(this, DashboardProveedor.class));
                        }

                        finish(); // Cierra MainActivity

                    } else {
                        // El usuario inició sesión pero no está registrado como comprador/proveedor
                        Toast.makeText(this, "No se encontró el usuario en " + coleccion, Toast.LENGTH_SHORT).show();
                        auth.signOut(); // Lo desconectamos para evitar inconsistencias
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al verificar rol: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
