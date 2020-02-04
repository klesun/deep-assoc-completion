<?php
class StoreController {
    protected $methodMap     = [
        self::ACTION_DEFAULT           => [ "self", "mainPage" ],
        self::ACTION_FINGERPRINT       => [ self::class, "fingerPrint" ],
        self::ACTION_CONTACTS          => [ "self", "contactPage" ],
        self::ACTION_TENDER_LOT_WIDGET => [ "self", "tenderLotWidget" ],
    ];

    const ololo = [self::class, 'doStaticStuff'];
    const ololo2 = [self::class, 'fingerPrint'];

    /**
     * Страница с контактной информацией
     *
     * @return $this
     */
    protected function contactPage() {
        $this->skinVars->title = "Контактная информация";
        $this->skinVars->noJs = true;
        $this->skinVars->jsNameOverride = "Contacts";

        return $this;
    }

    /**
     * Метод вызывается с любой страницы, если нету фингерпринта
     *
     * @return $this
     * @throws Exception
     */
    protected function fingerPrint() {
        $this->setSkin(SkinText::create());

        $this->vars->text = 1;

        $this->site->Session->setFingerPrint($this->site->getInputVarStr(self::PARAM_FINGERPRINT, 32));

        return $this;
    }

    private static function doStaticStuff() {

    }
}
