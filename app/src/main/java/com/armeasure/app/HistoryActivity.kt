package com.armeasure.app

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.armeasure.app.databinding.ActivityHistoryBinding

/**
 * HistoryActivity — আগের সব measurement history দেখায়।
 * RecyclerView-তে list আকারে সব measurement দেখা যায়।
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: MeasurementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        loadMeasurements()
    }

    private fun setupRecyclerView() {
        adapter = MeasurementAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadMeasurements() {
        val measurements = MeasurementStore.getAllMeasurements(this)
        adapter.submitList(measurements)

        if (measurements.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnClearAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("সব history মুছবে?")
                .setMessage("এটা undo করা যাবে না।")
                .setPositiveButton("হ্যাঁ, মুছো") { _, _ ->
                    MeasurementStore.clearAll(this)
                    loadMeasurements()
                    Toast.makeText(this, "History মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("না", null)
                .show()
        }
    }
}

/**
 * MeasurementAdapter — RecyclerView adapter যেটা measurement list render করে।
 */
class MeasurementAdapter : RecyclerView.Adapter<MeasurementAdapter.ViewHolder>() {

    private var items = listOf<MeasurementData>()

    fun submitList(list: List<MeasurementData>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_measurement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNumber: TextView = view.findViewById(R.id.tvNumber)
        private val tvDistance: TextView = view.findViewById(R.id.tvDistance)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(data: MeasurementData, number: Int) {
            tvNumber.text = "#$number"
            tvDistance.text = data.getFormattedDistance()
            tvTime.text = data.getFormattedTime()
        }
    }
}
