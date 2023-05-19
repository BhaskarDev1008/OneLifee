package com.geekymusketeers.medify.ui.mainFragments.home.appointment_booking

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.geekymusketeers.medify.R
import com.geekymusketeers.medify.base.ViewModelFactory
import com.geekymusketeers.medify.databinding.FragmentAppointmentBookingBinding
import com.geekymusketeers.medify.model.Summary
import com.geekymusketeers.medify.utils.Utils
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.ncorti.slidetoact.SlideToActView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


class AppointmentBookingFragment : Fragment() {

    private var _binding: FragmentAppointmentBookingBinding? = null
    private val binding get() = _binding!!
    private val args: AppointmentBookingFragmentArgs by navArgs()
    private lateinit var sharedPreference: SharedPreferences
    private lateinit var diseasesList: HashMap<String, ArrayList<String>>

    private val appointmentViewModel by viewModels<AppointmentBookingViewModel> {ViewModelFactory()}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setupObservers()
        setupListeners()
    }

    private fun initView() {
        sharedPreference = requireContext().getSharedPreferences("UserData", Context.MODE_PRIVATE)
        appointmentViewModel.initializeSpecializationWithDiseasesLists()
        appointmentViewModel.setDiseaseValues(Utils.setDiseaseValues(requireContext()))
    }

    private fun setupObservers() {
//        appointmentViewModel.navigateToBookingSummary.observe(viewLifecycleOwner) { summary ->
//            navigateToBookingSummary(summary)
//        }
        appointmentViewModel.run {
            getDataFromSharedPref(sharedPreference)
            fireStatusMutableLiveData.observe(
                viewLifecycleOwner
            ) { status ->
                if (status) {
                    navigateToBookingSummary(navigateToBookingSummary.value!!)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.selectDate.setOnClickListener {
            handleDatePicker()
        }
        diseasesList = Utils.initializeSpecializationWithDiseasesLists()
        val items: List<String> = diseasesList[args.doctorDetails.Specialist]!!
        val adapter = ArrayAdapter(requireContext(), R.layout.list_items, items)
        binding.diseaseDropdown.setAdapter(adapter)


        val situationItems = listOf("Severe Pain", "Mild Pain", "No Pain")
        val situationAdapter = ArrayAdapter(requireContext(), R.layout.list_items, situationItems)
        binding.situationDropdown.setAdapter(situationAdapter)

        val timeItems = listOf(
            "9:00 AM - 11:00 AM",
            "11:00 AM - 13:00 PM",
            "17:00 PM - 19:00 PM",
            "19:00 PM - 22:OO PM"
        )
        val timeAdapter = ArrayAdapter(requireContext(), R.layout.list_items, timeItems)
        binding.timeDropdown.setAdapter(timeAdapter)

        binding.btnFinalbook.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
            override fun onSlideComplete(view: SlideToActView) {
                val doctorType = args.doctorDetails.Specialist
                val selectDate = binding.selectDate.text.toString()
                val time = binding.timeDropdown.text.toString()
                val disease = binding.diseaseDropdown.text.toString()
                val situation = binding.situationDropdown.text.toString()

                appointmentViewModel.bookAppointment(
                    doctorType,
                    selectDate,
                    time,
                    disease,
                    situation,
                    args.doctorDetails.UID!!,
                    args.doctorDetails.Name!!,
                    args.doctorDetails.Email!!,
                    args.doctorDetails.Phone!!,
                    Utils.setConditionValue(requireContext())
                )
            }
        }
    }

    private fun handleDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker().apply {

            // disable past dates
            val constraintsBuilder = CalendarConstraints.Builder()
            constraintsBuilder.setValidator(DateValidatorPointForward.now())
            setCalendarConstraints(constraintsBuilder.build())

            // set the minimum selectable date to today's date
            val calendar = Calendar.getInstance()
            setSelection(calendar.timeInMillis)

        }.build()
        datePicker.show(requireActivity().supportFragmentManager, "DatePicker")


        datePicker.addOnPositiveButtonClickListener {
            val dateFormatter = SimpleDateFormat("dd-MM-yyyy")
            val date = dateFormatter.format(Date(it))
            binding.selectDate.setText(date)
        }
    }

    private fun navigateToBookingSummary(summary: Summary) {
        val action =
            AppointmentBookingFragmentDirections.actionAppointmentBookingFragmentToBookingSummaryFragment(
                summary
            )
        findNavController().navigate(action)
    }
}