package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.buildings.esia

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.R
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapMatrixProvider
import ovh.gabrielhuav.sensores_escom_v2.presentation.game.mapview.MapView
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.managers.MovementManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.common.components.UIManager
import ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor.ESIA

class EdificioNuevoESIA : AppCompatActivity(), MapView.MapTransitionListener {

    private lateinit var movementManager: MovementManager
    private lateinit var uiManager: UIManager
    private lateinit var mapView: MapView
    private lateinit var playerName: String
    private var isServer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_esia)

        playerName = intent.getStringExtra("PLAYER_NAME") ?: "Invitado"
        isServer = intent.getBooleanExtra("IS_SERVER", false)

        mapView = MapView(context = this, mapResourceId = R.drawable.edificio_nuevo_esia)
        findViewById<FrameLayout>(R.id.map_container).addView(mapView)

        uiManager = UIManager(findViewById(R.id.main_layout), mapView)
        uiManager.initializeViews()

        movementManager = MovementManager(mapView) { position ->
            mapView.updateLocalPlayerPosition(position, forceCenter = true)
        }

        mapView.setMapTransitionListener(this)
        setupButtonListeners()

        mapView.post {
            val mapKey = MapMatrixProvider.MAP_EDIFICIO_NUEVO_ESIA
            mapView.setCurrentMap(mapKey, R.drawable.edificio_nuevo_esia)
            mapView.playerManager.localPlayerId = playerName

            // Si venimos del salón, regresamos a la puerta. Si no, aparecemos en la entrada principal.
            val returnX = intent.getIntExtra("SALON_RETURN_X", 20)
            val returnY = intent.getIntExtra("SALON_RETURN_Y", 35)
            val startPos = if (intent.getBooleanExtra("RETURN_FROM_SALON", false)) Pair(returnX, returnY) else Pair(20, 35)

            movementManager.setPosition(startPos)
            mapView.updateLocalPlayerPosition(startPos, forceCenter = true)
        }
    }

    private fun setupButtonListeners() {
        uiManager.setupMovementButtons(movementManager)
        uiManager.btnNorth.setOnTouchListener { _, event -> movementManager.handleMovement(event, 0, -1); true }
        uiManager.btnSouth.setOnTouchListener { _, event -> movementManager.handleMovement(event, 0, 1); true }
        uiManager.btnEast.setOnTouchListener { _, event -> movementManager.handleMovement(event, 1, 0); true }
        uiManager.btnWest.setOnTouchListener { _, event -> movementManager.handleMovement(event, -1, 0); true }

        uiManager.buttonA.setOnClickListener {
            val currentPos = mapView.playerManager.getLocalPlayerPosition()
            if (currentPos != null) {
                val transition = MapMatrixProvider.isMapTransitionPoint(MapMatrixProvider.MAP_EDIFICIO_NUEVO_ESIA, currentPos.first, currentPos.second)
                if (transition != null) {
                    onMapTransitionRequested(transition, currentPos)
                } else {
                    Toast.makeText(this, "Acércate a una puerta para salir", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onMapTransitionRequested(targetMap: String, initialPosition: Pair<Int, Int>) {
        when (targetMap) {
            MapMatrixProvider.MAP_ESIA -> {
                val intent = Intent(this, ESIA::class.java).apply {
                    putExtra("PLAYER_NAME", playerName)
                    putExtra("IS_SERVER", isServer)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                mapView.playerManager.cleanup()
                startActivity(intent)
                finish()
            }
            // ✅ AQUÍ JALAMOS LA ACTIVITY DEL SALÓN
            MapMatrixProvider.MAP_SALON_ESIA -> {
                val intent = Intent(this, SalonESIA::class.java).apply {
                    putExtra("PLAYER_NAME", playerName)
                    putExtra("IS_SERVER", isServer)
                    putExtra("INITIAL_POSITION", Pair(20, 20)) // Entramos al medio del salón
                    // Guardamos la puerta en la que estábamos para regresar ahí
                    putExtra("RETURN_X", initialPosition.first)
                    putExtra("RETURN_Y", initialPosition.second + 1) // +1 para no re-chocar con la puerta al volver
                    putExtra("FROM_NUEVO_EDIFICIO", true) // Le decimos que venimos de aquí
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                mapView.playerManager.cleanup()
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        movementManager.stopMovement()
    }
}