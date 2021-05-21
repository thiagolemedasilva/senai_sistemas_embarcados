/*
   This example code is in the Public Domain (or CC0 licensed, at your option.)

   Unless required by applicable law or agreed to in writing, this
   software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
   CONDITIONS OF ANY KIND, either express or implied.
*/

#include <stdint.h>
#include <string.h>
#include <stdbool.h>
#include <stdio.h>
#include "nvs.h"
#include "nvs_flash.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "esp_bt.h"
#include "esp_bt_main.h"
#include "esp_gap_bt_api.h"
#include "esp_bt_device.h"
#include "esp_spp_api.h"

#include "time.h"
#include "sys/time.h"

//GPIO
#include "driver/gpio.h"
#include "freertos/queue.h"
#define GPIO_INPUT_IO_0     4
#define GPIO_INPUT_IO_1     5
#define GPIO_INPUT_PIN_SEL  ((1ULL<<GPIO_INPUT_IO_0) | (1ULL<<GPIO_INPUT_IO_1))
#define ESP_INTR_FLAG_DEFAULT 0

//ADC
#include "driver/adc.h"
#include "esp_adc_cal.h"

#define DEFAULT_VREF    1100        //Use adc2_vref_to_gpio() to obtain a better estimate
#define NO_OF_SAMPLES   64          //Multisampling

static esp_adc_cal_characteristics_t *adc_chars;
static const adc_channel_t channel = ADC_CHANNEL_6;     //GPIO34 if ADC1, GPIO14 if ADC2
static const adc_atten_t atten = ADC_ATTEN_DB_11;
static const adc_unit_t unit = ADC_UNIT_1;


#define SPP_TAG "SPP_ACCEPTOR_DEMO"
#define SPP_SERVER_NAME "SPP_SERVER"
#define EXCAMPLE_DEVICE_NAME "ESP32"
#define SPP_SHOW_DATA 0
#define SPP_SHOW_SPEED 1
#define SPP_SHOW_MODE SPP_SHOW_DATA    /*Choose show mode: show data or speed*/

static const esp_spp_mode_t esp_spp_mode = ESP_SPP_MODE_CB;

static struct timeval time_new, time_old;

static const esp_spp_sec_t sec_mask = ESP_SPP_SEC_AUTHENTICATE;
static const esp_spp_role_t role_slave = ESP_SPP_ROLE_SLAVE;

#if (SPP_SHOW_MODE == SPP_SHOW_DATA)
#define SPP_DATA_LEN 10
#else
#define SPP_DATA_LEN ESP_SPP_MAX_MTU
#endif
static uint8_t spp_data[SPP_DATA_LEN];
static int16_t hist[30][24][4];
static uint8_t msg_val[16384];
int msg_len = 0;
static uint8_t tp[6];
int tp_len = 0;
int dia_env = 0;
bool inicializado = false;
//Valores ref
int r_ano = 0;
int r_mes = 0;
int r_dia = 0;
int r_hora = 0;
int r_min = 0;
int r_seg = 0;
bool first_ref = true;
int interval_ref = 0;
//Valores
int dia = 0;
int hora = 0;
int16_t esq = 0;
int16_t dir = 0;
int16_t falha = 0;
int16_t tensao = 0;
//Erro
int16_t count_falha = 0;
int16_t ver_falha = 0;
int inc_leitura = 0;
void reset_array_his(){
	
	gettimeofday(&time_new, NULL);
	time_old.tv_sec = time_new.tv_sec;
	
	dia = 0;
	hora = 0;
	esq = 0;
	dir = 0;
	falha = 0;
	tensao = 0;
	
	count_falha = 0;
	ver_falha = 0;
	
	for(int i = 0; i < 30; i++){
		for(int j = 0; j < 24; j++){
			for(int k = 0; k < 4; k++){
				hist[i][j][k] = -1;
			}
		}
	}
}
void update_hour(){
	
	hist[dia][hora][0] = esq;
	hist[dia][hora][1] = dir;
	hist[dia][hora][2] = falha;
	hist[dia][hora][3] = tensao;
}
void inc_hour(){
	
	hora ++;
	if(hora == 24){
		hora = 0;
		dia++;
		if(dia == 30){
			dia = 0;
			inicializado = false;
		}			
	}
	
	esq = 0;
	dir = 0;
	falha = 0;
	tensao = 0;
	
}

bool checkIncHour(){
	gettimeofday(&time_new, NULL);
	if ((time_new.tv_sec - time_old.tv_sec) >= interval_ref) {
		time_old.tv_sec = time_old.tv_sec + interval_ref;
		interval_ref = 3600;
		return true;
	}else{
		return false;
	}
	
}
void intoarray (int value)
{
	
    char temp;
    int i =0;
	bool negativo = false;
	if (value == 0)
	{
		tp[i] = '0';
		i++;
	}
	if(value < 0){
		negativo = true;		
		value = -1 * value;		
	}	
    while (value > 0) {
        int digito = value % 10;
        tp[i] = digito + '0';
        value /= 10;
        i++;
    }
   
	if(negativo){
		tp[i] = '-';
		i++;
	}
	int j = i-1;
	tp_len = i;
	i = 0;
	while (i < j) {
	  temp = tp[i];
	  tp[i] = tp[j];
	  tp[j] = temp;
	  i++;
	  j--;
	}
}
void gerar_string_atual(){
	
	int t_esq = -1;
	int t_dir = -1;
	int t_falha = -1;
	int t_tensao = -1;
	
	if(inicializado){
		
		t_esq = 0;
		t_dir = 0;
		t_falha = 0;
		t_tensao = 0;
		
		update_hour();
		
		for(int i = 0; i < 30; i++){
			for(int j = 0; j < 24; j++){
				if(hist[i][j][0] >= 0){
					t_esq += hist[i][j][0];
					t_dir += hist[i][j][1];
					t_falha += hist[i][j][2];
					if(hist[i][j][3] > t_tensao){
						t_tensao = hist[i][j][3];
					}					
				}
			}
		}
	}
	
	int index = 0;	
	
	msg_val[index] = 'F';
	index++;
	
	//Valores das variáveis
	intoarray (t_esq);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	msg_val[index] = ',';
	index++;
	intoarray (t_dir);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	msg_val[index] = ',';
	index++;
	
	intoarray (t_falha);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	msg_val[index] = ',';
	index++;
	
	intoarray (t_tensao);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	msg_val[index] = ',';
	index++;
	
	//Valores de data inicial
	intoarray (r_ano);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	msg_val[index] = ',';
	index++;
	
	intoarray (r_mes);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	msg_val[index] = ',';
	index++;
	
	intoarray (r_dia);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	msg_val[index] = ',';
	index++;
	
	intoarray (r_hora);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	msg_val[index] = ',';
	index++;
	
	intoarray (r_min);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	msg_val[index] = ',';
	index++;
	
	intoarray (r_seg);
	for (int z = 0; z < tp_len; z++)
	{
		msg_val[index] = tp[z];
		index++;                         
	}
	
	msg_val[index] = '\0';
	msg_len = index;
}
void gerar_string_dia(){
	
	int index = 0;
	//Gerando matriz
	msg_val[index] = '[';
	index++;
	msg_val[index] = '[';
	index++;
	for (int j = 0; j < 24; j++)
	{
		msg_val[index] = '[';
		index++;
		for (int k = 0; k < 4; k++)
		{
			intoarray (hist[dia_env][j][k]);
			for (int z = 0; z < tp_len; z++)
			{
				msg_val[index] = tp[z];
				index++;                         
			}
			if (k < 3)
			{
				msg_val[index] = ',';
				index++;
			} 
		}
		msg_val[index] = ']';
		index++;
		if (j < 23)
		{
			msg_val[index] = ',';
			index++;
		}
	}
	msg_val[index] = ']';
	index++;
	
	msg_val[index] = ']';
	index++;
	msg_val[index] = '\0';
	msg_len = index;
}

static void check_efuse()
{
    //Check TP is burned into eFuse
    if (esp_adc_cal_check_efuse(ESP_ADC_CAL_VAL_EFUSE_TP) == ESP_OK) {
        printf("eFuse Two Point: Supported\n");
    } else {
        printf("eFuse Two Point: NOT supported\n");
    }
    //Check Vref is burned into eFuse
    if (esp_adc_cal_check_efuse(ESP_ADC_CAL_VAL_EFUSE_VREF) == ESP_OK) {
        printf("eFuse Vref: Supported\n");
    } else {
        printf("eFuse Vref: NOT supported\n");
    }
}

static void print_char_val_type(esp_adc_cal_value_t val_type)
{
    if (val_type == ESP_ADC_CAL_VAL_EFUSE_TP) {
        printf("Characterized using Two Point Value\n");
    } else if (val_type == ESP_ADC_CAL_VAL_EFUSE_VREF) {
        printf("Characterized using eFuse Vref\n");
    } else {
        printf("Characterized using Default Vref\n");
    }
}


void updateDataRef(uint8_t *s){

	r_ano = 0;
	r_mes = 0;
	r_dia = 0;
	r_hora = 0;
	r_min = 0;
	r_seg = 0;
	
	r_ano += (s[1] - '0') * 1000;
	r_ano += (s[2] - '0') * 100;
	r_ano += (s[3] - '0') * 10;
	r_ano += s[4] - '0';
	
	r_mes += (s[5] - '0') * 10;
	r_mes += s[6] - '0';
	
	r_dia += (s[7] - '0') * 10;
	r_dia += s[8] - '0';
	
	r_hora += (s[9] - '0') * 10;
	r_hora += s[10] - '0';
	
	r_min += (s[11] - '0') * 10;
	r_min += s[12] - '0';
	
	r_seg += (s[13] - '0') * 10;
	r_seg += s[14] - '0';
	
	hora = r_hora;
	interval_ref = 60 - r_seg + (59 - r_min) * 60;
	
}

static void esp_spp_cb(esp_spp_cb_event_t event, esp_spp_cb_param_t *param)
{
    switch (event) {
    case ESP_SPP_INIT_EVT:
        ESP_LOGI(SPP_TAG, "ESP_SPP_INIT_EVT");
        esp_bt_dev_set_device_name(EXCAMPLE_DEVICE_NAME);
        esp_bt_gap_set_scan_mode(ESP_BT_CONNECTABLE, ESP_BT_GENERAL_DISCOVERABLE);
		//esp_bt_gap_set_scan_mode(ESP_BT_SCAN_MODE_CONNECTABLE_DISCOVERABLE);
        esp_spp_start_srv(sec_mask,role_slave, 0, SPP_SERVER_NAME);
        break;
    case ESP_SPP_DISCOVERY_COMP_EVT:
        ESP_LOGI(SPP_TAG, "ESP_SPP_DISCOVERY_COMP_EVT");
        break;
    case ESP_SPP_OPEN_EVT:
        ESP_LOGI(SPP_TAG, "ESP_SPP_OPEN_EVT");
		esp_log_buffer_hex("",spp_data,SPP_DATA_LEN);
		esp_spp_write(param->srv_open.handle, SPP_DATA_LEN, spp_data);
        break;
    case ESP_SPP_CLOSE_EVT:
        ESP_LOGI(SPP_TAG, "ESP_SPP_CLOSE_EVT");
        break;
    case ESP_SPP_START_EVT:
        ESP_LOGI(SPP_TAG, "ESP_SPP_START_EVT");
        break;
    case ESP_SPP_CL_INIT_EVT:
        ESP_LOGI(SPP_TAG, "ESP_SPP_CL_INIT_EVT");
        break;
    case ESP_SPP_DATA_IND_EVT:
	
		ESP_LOGI(SPP_TAG, "ESP_SPP_DATA_IND_EVT len=%d handle=%d",
                 param->data_ind.len, param->data_ind.handle);
        esp_log_buffer_hex("",param->data_ind.data,param->data_ind.len);
		esp_log_buffer_char("",param->data_ind.data,param->data_ind.len);
		if(param->data_ind.data[0] == 't'){
			inicializado = false;
			reset_array_his();
			updateDataRef(param->data_ind.data);
			inicializado = true;
		}
		if(param->data_ind.data[0] == 'e'){
			
			update_hour();
			if(dia_env <= dia){
				gerar_string_dia();
				esp_spp_write(param->write.handle, msg_len, msg_val);
				dia_env++;
			}else{
				//Já foi finalizado, enviar confirmação
				gerar_string_atual();
				esp_spp_write(param->write.handle, msg_len, msg_val);
				dia_env = 0;
			}		
			
		}
		
		if(param->data_ind.data[0] == 'f'){
			gerar_string_atual();
			esp_spp_write(param->write.handle, msg_len, msg_val);			
		}        
		
		break;
    case ESP_SPP_CONG_EVT:
        ESP_LOGI(SPP_TAG, "ESP_SPP_CONG_EVT");
        break;
    case ESP_SPP_WRITE_EVT:
        ESP_LOGI(SPP_TAG, "ESP_SPP_WRITE_EVT");
        break;
    case ESP_SPP_SRV_OPEN_EVT:
        ESP_LOGI(SPP_TAG, "ESP_SPP_SRV_OPEN_EVT");

        break;
    default:
        break;
    }
}

void esp_bt_gap_cb(esp_bt_gap_cb_event_t event, esp_bt_gap_cb_param_t *param)
{
    switch (event) {
    case ESP_BT_GAP_AUTH_CMPL_EVT:{
        if (param->auth_cmpl.stat == ESP_BT_STATUS_SUCCESS) {
            ESP_LOGI(SPP_TAG, "authentication success: %s", param->auth_cmpl.device_name);
            esp_log_buffer_hex(SPP_TAG, param->auth_cmpl.bda, ESP_BD_ADDR_LEN);
        } else {
            ESP_LOGE(SPP_TAG, "authentication failed, status:%d", param->auth_cmpl.stat);
        }
        break;
    }
    case ESP_BT_GAP_PIN_REQ_EVT:{
        ESP_LOGI(SPP_TAG, "ESP_BT_GAP_PIN_REQ_EVT min_16_digit:%d", param->pin_req.min_16_digit);
        if (param->pin_req.min_16_digit) {
            ESP_LOGI(SPP_TAG, "Input pin code: 0000 0000 0000 0000");
            esp_bt_pin_code_t pin_code = {0};
            esp_bt_gap_pin_reply(param->pin_req.bda, true, 16, pin_code);
        } else {
            ESP_LOGI(SPP_TAG, "Input pin code: 1234");
            esp_bt_pin_code_t pin_code;
            pin_code[0] = '1';
            pin_code[1] = '2';
            pin_code[2] = '3';
            pin_code[3] = '4';
            esp_bt_gap_pin_reply(param->pin_req.bda, true, 4, pin_code);
        }
        break;
    }

#if (CONFIG_BT_SSP_ENABLED == true)
    case ESP_BT_GAP_CFM_REQ_EVT:
        ESP_LOGI(SPP_TAG, "ESP_BT_GAP_CFM_REQ_EVT Please compare the numeric value: %d", param->cfm_req.num_val);
        esp_bt_gap_ssp_confirm_reply(param->cfm_req.bda, true);
        break;
    case ESP_BT_GAP_KEY_NOTIF_EVT:
        ESP_LOGI(SPP_TAG, "ESP_BT_GAP_KEY_NOTIF_EVT passkey:%d", param->key_notif.passkey);
        break;
    case ESP_BT_GAP_KEY_REQ_EVT:
        ESP_LOGI(SPP_TAG, "ESP_BT_GAP_KEY_REQ_EVT Please enter passkey!");
        break;
#endif

    default: {
        ESP_LOGI(SPP_TAG, "event: %d", event);
        break;
    }
    }
    return;
}

//GPIO
static xQueueHandle gpio_evt_queue = NULL;
static void IRAM_ATTR gpio_isr_handler(void* arg)
{
    uint32_t gpio_num = (uint32_t) arg;
    xQueueSendFromISR(gpio_evt_queue, &gpio_num, NULL);
}
static void gpio_task_example(void* arg)
{
    uint32_t io_num;
    for(;;) {
        if(xQueueReceive(gpio_evt_queue, &io_num, portMAX_DELAY)) {
            printf("GPIO[%d] intr, val: %d\n", io_num, gpio_get_level(io_num));
			
			//Deixa a saída com o valor lido na entrada
			int niv = gpio_get_level(io_num);
			if(niv == 0){
				ver_falha = 0;
				if(inc_leitura > 1){
					if(io_num == 4){
						esq++;
					}else if(io_num == 5){	
						dir++;
					}
					inc_leitura = 0;
				}
			}else{
				count_falha = 0;
				ver_falha = 1;				
			}
        }
    }
}

int32_t converter_tensao(int32_t v){
	double vl = ((double)v)/1000.0  * 264.289474 - 335.750789;
	int32_t res = (int32_t)vl;
	
	return res;
}

void app_main()
{
	//Inicia matriz de armazenamento
	reset_array_his();
	
	//Inicialização do bluetooth
	
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK( ret );

    ESP_ERROR_CHECK(esp_bt_controller_mem_release(ESP_BT_MODE_BLE));

    esp_bt_controller_config_t bt_cfg = BT_CONTROLLER_INIT_CONFIG_DEFAULT();
    if ((ret = esp_bt_controller_init(&bt_cfg)) != ESP_OK) {
        ESP_LOGE(SPP_TAG, "%s initialize controller failed: %s\n", __func__, esp_err_to_name(ret));
        return;
    }

    if ((ret = esp_bt_controller_enable(ESP_BT_MODE_CLASSIC_BT)) != ESP_OK) {
        ESP_LOGE(SPP_TAG, "%s enable controller failed: %s\n", __func__, esp_err_to_name(ret));
        return;
    }

    if ((ret = esp_bluedroid_init()) != ESP_OK) {
        ESP_LOGE(SPP_TAG, "%s initialize bluedroid failed: %s\n", __func__, esp_err_to_name(ret));
        return;
    }

    if ((ret = esp_bluedroid_enable()) != ESP_OK) {
        ESP_LOGE(SPP_TAG, "%s enable bluedroid failed: %s\n", __func__, esp_err_to_name(ret));
        return;
    }

    if ((ret = esp_bt_gap_register_callback(esp_bt_gap_cb)) != ESP_OK) {
        ESP_LOGE(SPP_TAG, "%s gap register failed: %s\n", __func__, esp_err_to_name(ret));
        return;
    }

    if ((ret = esp_spp_register_callback(esp_spp_cb)) != ESP_OK) {
        ESP_LOGE(SPP_TAG, "%s spp register failed: %s\n", __func__, esp_err_to_name(ret));
        return;
    }

    if ((ret = esp_spp_init(esp_spp_mode)) != ESP_OK) {
        ESP_LOGE(SPP_TAG, "%s spp init failed: %s\n", __func__, esp_err_to_name(ret));
        return;
    }

#if (CONFIG_BT_SSP_ENABLED == true)
    /* Set default parameters for Secure Simple Pairing */
    esp_bt_sp_param_t param_type = ESP_BT_SP_IOCAP_MODE;
    esp_bt_io_cap_t iocap = ESP_BT_IO_CAP_IO;
    esp_bt_gap_set_security_param(param_type, &iocap, sizeof(uint8_t));
#endif

    /*
     * Set default parameters for Legacy Pairing
     * Use variable pin, input pin code when pairing
     */
    esp_bt_pin_type_t pin_type = ESP_BT_PIN_TYPE_VARIABLE;
    esp_bt_pin_code_t pin_code;
    esp_bt_gap_set_pin(pin_type, 0, pin_code);
	
	//Inicialização de GPIO
	gpio_config_t io_conf;
    
    //interrupt of rising edge
    io_conf.intr_type = GPIO_PIN_INTR_POSEDGE;
    //bit mask of the pins, use GPIO4/5 here
    io_conf.pin_bit_mask = GPIO_INPUT_PIN_SEL;
    //set as input mode    
    io_conf.mode = GPIO_MODE_INPUT;
    //enable pull-up mode
    io_conf.pull_up_en = 1;
    gpio_config(&io_conf);
    //change gpio intrrupt type for one pin
    gpio_set_intr_type(GPIO_INPUT_IO_0, GPIO_INTR_ANYEDGE);
	gpio_set_intr_type(GPIO_INPUT_IO_1, GPIO_INTR_ANYEDGE);
    //create a queue to handle gpio event from isr
    gpio_evt_queue = xQueueCreate(10, sizeof(uint32_t));
    //start gpio task
    xTaskCreate(gpio_task_example, "gpio_task_example", 2048, NULL, 10, NULL);
    //install gpio isr service
    gpio_install_isr_service(ESP_INTR_FLAG_DEFAULT);
    
	//hook isr handler for specific gpio pin
    gpio_isr_handler_add(GPIO_INPUT_IO_0, gpio_isr_handler, (void*) GPIO_INPUT_IO_0);
    //hook isr handler for specific gpio pin
    gpio_isr_handler_add(GPIO_INPUT_IO_1, gpio_isr_handler, (void*) GPIO_INPUT_IO_1);
    
	//remove isr handler for gpio number.
    gpio_isr_handler_remove(GPIO_INPUT_IO_0);
    //hook isr handler for specific gpio pin again
    gpio_isr_handler_add(GPIO_INPUT_IO_0, gpio_isr_handler, (void*) GPIO_INPUT_IO_0);
	
	//remove isr handler for gpio number.
	gpio_isr_handler_remove(GPIO_INPUT_IO_1);
	//hook isr handler for specific gpio pin again
    gpio_isr_handler_add(GPIO_INPUT_IO_1, gpio_isr_handler, (void*) GPIO_INPUT_IO_1);
	
	
	
//ADC
	//Check if Two Point or Vref are burned into eFuse
    check_efuse();
    //Configure ADC
    if (unit == ADC_UNIT_1) {
        adc1_config_width(ADC_WIDTH_BIT_12);
        adc1_config_channel_atten(channel, atten);
    } else {
        adc2_config_channel_atten((adc2_channel_t)channel, atten);
    }
    //Characterize ADC
    adc_chars = calloc(1, sizeof(esp_adc_cal_characteristics_t));
    esp_adc_cal_value_t val_type = esp_adc_cal_characterize(unit, atten, ADC_WIDTH_BIT_12, DEFAULT_VREF, adc_chars);
    print_char_val_type(val_type);
    while(1) {
		
		//ADC
		uint32_t adc_reading = 0;
		uint32_t max_vol = 0;
		for (int i = 0; i < NO_OF_SAMPLES; i++) {
            if (unit == ADC_UNIT_1) {
                adc_reading = adc1_get_raw((adc1_channel_t)channel);
            } else {
                int raw;
                adc2_get_raw((adc2_channel_t)channel, ADC_WIDTH_BIT_12, &raw);
                adc_reading = raw;
            }
			
			if  (adc_reading > max_vol)  {
			  max_vol = adc_reading;
			}
			
			
			vTaskDelay(1 / portTICK_RATE_MS);			
        }

		max_vol = esp_adc_cal_raw_to_voltage(max_vol, adc_chars);
		uint32_t voltage = converter_tensao(max_vol);

		if(inicializado){
			
			if(voltage > tensao){
				tensao = voltage;
			}
			
			if(ver_falha == 1){
				//Considerando delay de 1s no loop
				count_falha++;
				if(count_falha >= 10){
					ver_falha = 0;
					count_falha = 0;
					falha++;
				}
			}
			
			if(checkIncHour()){
				update_hour();
				inc_hour();
			}
			inc_leitura++;
		}
		
        vTaskDelay(936 / portTICK_RATE_MS); //Desconta o tempo do conversor ADC
    }
}

