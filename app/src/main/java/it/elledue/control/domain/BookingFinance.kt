package it.elledue.control.domain

/** EUR cents, HALF_UP separately on base commission, VAT and management fee. */
data class BookingTotals(val otaCents:Long,val netCents:Long,val managementCents:Long,val baseOtaCents:Long=otaCents,val vatCents:Long=0)
object OtaTreatment {
    val values=listOf("BASE_PLUS_VAT","INCLUDED","NONE")
    fun label(mode:String?)=when(mode){"BASE_PLUS_VAT"->"Commissione base + IVA";"INCLUDED"->"Importo già IVA compresa";else->"Senza maggiorazione IVA"}
}
object BookingFinance {
    fun calculate(gross:Long,otaBasisPoints:Int,actualOta:Long?,managementBasisPoints:Int,treatment:String?="NONE",vatBasisPoints:Int=2200):BookingTotals {
        require(gross>=0){"Il lordo non può essere negativo."}
        require(otaBasisPoints in 0..10000 && managementBasisPoints in 0..10000 && vatBasisPoints in 0..10000){"Percentuali non valide."}
        require(treatment==null || treatment in OtaTreatment.values){"Trattamento OTA non valido."}
        val base=actualOta ?: Amounts.percentage(gross,otaBasisPoints)
        require(base>=0){"La commissione non può essere negativa."}
        val vat=if(treatment=="BASE_PLUS_VAT")Amounts.percentage(base,vatBasisPoints) else 0
        val ota=Math.addExact(base,vat)
        require(ota<=gross){"Il costo OTA complessivo non può superare il lordo."}
        val net=gross-ota
        return BookingTotals(ota,net,Amounts.percentage(net,managementBasisPoints),base,vat)
    }
    fun calculate(b:it.elledue.control.data.ReservationEntity)=calculate(b.grossCents,b.appliedOtaBasisPoints,b.actualOtaCents,b.appliedManagementBasisPoints,b.appliedOtaTreatment,b.appliedOtaVatBasisPoints)
}
