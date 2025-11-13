

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔹 1) Deja la barra de estado (status bar) transparente
        // Esto hace que el contenido pueda dibujarse debajo de la barra superior
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // 🔹 2) Ajusta el layout para que ocupe toda la pantalla
        // SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN → El layout sube hasta arriba (debajo del notch)
        // SYSTEM_UI_FLAG_LAYOUT_STABLE → Mantiene estable el tamaño al cambiar visibilidad de UI
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);

        // 🔹 3) Busca si el layout tiene un header con el id "llHeader"
        // Esto permite que TODAS las actividades hereden este comportamiento sin duplicar código
        View header = findViewById(R.id.llHeader);

        if (header != null) {

            // 🔹 4) Obtiene la altura real de la barra de estado del sistema Android
            // "status_bar_height" es un recurso interno del sistema
            int statusBarHeightId = getResources().getIdentifier("status_bar_height", "dimen", "android");

            // Si existe ese recurso…
            if (statusBarHeightId > 0) {

                // 🔹 5) Convierte ese recurso a pixeles reales
                int statusBarHeight = getResources().getDimensionPixelSize(statusBarHeightId);

                // 🔹 6) Aumenta el padding superior del header
                // Esto mueve el header hacia abajo para que NO quede debajo de la barra de estado
                header.setPadding(
                        header.getPaddingLeft(),
                        header.getPaddingTop() + statusBarHeight,  // Aquí está el truco
                        header.getPaddingRight(),
                        header.getPaddingBottom()
                );
            }
        }
    }
}
