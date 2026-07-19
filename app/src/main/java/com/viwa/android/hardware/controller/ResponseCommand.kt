package com.viwa.android.hardware.controller

/** Входящие ответы (RX),ts]. */
@Suppress("EnumEntryName", "SpellCheckingInspection")
enum class ResponseCommand(val code: Int) {
    Undefined(0x00),
    XS(0x11),
    PaymentSystemsExist(0x18),
    CoinOrCashAnswer(0x20),
    ControllerACK(0x22),
    ControllerVersionAnswer(0x49),
    ControllerChoosedDrinkACK(0x50),
    DrinkPreparingSuccess(0x51),
    ControllerMsgError(0x52),
    PaymentSystemsGotDeposite(0x53),
    DrinkPreparingBegin(0x54),
    AutoChangeDeviceMode(0x55),
    PaymentSystemsPaxStatus(0x56),
    DrinkPreparingTakeoff(0x57),
    RFIDBracerTimeout(0x58),
    RFIDBracerUID(0x60),
    StepCountAnswer(0x62),
    CurrencyNumberAnswer(0x63),
    RFIDVersionAnswer(0x65),
    MDBLevelAnswer(0x66),
    ReadSensorStatus(0x67),
    SystemsShutdownFromUPS(0x68),
    ControllerAutoWash(0x69),
    DoesntTakeCupError(0x70),
    WashAfterEveryShakeAnswer(0x71),
    MDBLogsAnswer(0x72),
    BucketIsFull(0x73),
    WaterCounterAnswer(0x74),
    ControllerResetButtonClick(0x75),
    ControllerTimeoutResetActivate(0x76),
    CupSensorStatusAnswer(0x77),
    HandModuleStatusAnswer(0x80),
    IsCupInWindow(0x81),
    InvertCupSensorAnswer(0x87),
    WaterPumpModelAnswer(0x88),
    ItalianKeyDeposit(0x89),
    SnackDoorOpenWithPaymentDevice(0x90),
    SnackSensorStatus(0x92),
    SnackTemperature(0x93),
    ControllerInfoMessage(0x94),
    ;

    companion object {
        private val byCode: Map<Int, ResponseCommand> = entries.associateBy { it.code }

        fun fromCode(code: Int): ResponseCommand? = byCode[code]
    }
}
