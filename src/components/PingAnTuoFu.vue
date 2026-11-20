<script setup>
import { computed, nextTick, ref } from 'vue'

const props = defineProps({
  maxContacts: { type: Number, default: 3 }
})

const emit = defineEmits(['submit'])

const pad = n => String(n).padStart(2, '0')
const toISO = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const shortDate = iso => {
  const [, m, d] = iso.split('-')
  return `${+m}月${+d}日`
}

const today = toISO(new Date())

let seq = 1
const date = ref(today)
const contacts = ref([{ id: seq++, name: '', phone: '' }])
const note = ref('')
const formError = ref('')
const errors = ref({ date: false, items: {} })
const records = ref([])
const dialogOpen = ref(false)
const pending = ref(null)
const toastVisible = ref(false)
const toastText = ref('')

const dateInput = ref(null)
const scrollEl = ref(null)

const dateLabel = computed(() => {
  if (!date.value) return '—'
  const [y, m, d] = date.value.split('-')
  return `${y}年${+m}月${+d}日`
})

const canAdd = computed(() => contacts.value.length < props.maxContacts)

function openPicker() {
  if (dateInput.value && typeof dateInput.value.showPicker === 'function') {
    try {
      dateInput.value.showPicker()
    } catch (e) {
      /* 部分浏览器不支持，点击原生 input 即可 */
    }
  }
}

async function addContact() {
  if (!canAdd.value) return
  contacts.value.push({ id: seq++, name: '', phone: '' })
  await nextTick()
  const rows = scrollEl.value?.querySelectorAll('.contact-row')
  rows?.[rows.length - 1]?.querySelector('.name')?.focus()
}

function removeContact(id) {
  if (contacts.value.length <= 1) return
  contacts.value = contacts.value.filter(c => c.id !== id)
}

function onPhoneInput(contact, event) {
  const clean = event.target.value.replace(/\D/g, '').slice(0, 11)
  contact.phone = clean
  event.target.value = clean
  clearFieldError(contact.id, 'phone')
}

function clearFieldError(id, field) {
  const item = errors.value.items[id]
  if (item && item[field]) item[field] = false
  if (field === 'name') formError.value = ''
}

function validate() {
  errors.value = { date: false, items: {} }

  if (!date.value || date.value < today) {
    errors.value.date = true
    return '请选择今天或之后的生效日期'
  }

  const filled = contacts.value.filter(c => c.name.trim() || c.phone.trim())
  if (!filled.length) return '请至少填写一位家属的称呼和手机号'

  let bad = false
  filled.forEach(c => {
    const nameBad = !c.name.trim()
    const phoneBad = !/^1[3-9]\d{9}$/.test(c.phone.trim())
    if (nameBad || phoneBad) {
      bad = true
      errors.value.items[c.id] = { name: nameBad, phone: phoneBad }
    }
  })

  return bad ? '请填写完整信息，手机号需为 11 位有效号码' : ''
}

function submitRequest() {
  const msg = validate()
  if (msg) {
    formError.value = msg
    return
  }

  formError.value = ''
  const filled = contacts.value.filter(c => c.name.trim() || c.phone.trim())
  pending.value = {
    date: date.value,
    contacts: filled.map(c => ({ name: c.name.trim(), phone: c.phone.trim() })),
    note: note.value.trim()
  }
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
}

function confirmSubmit() {
  if (!pending.value) return

  const payload = pending.value
  const now = new Date()

  records.value.unshift({
    id: Date.now(),
    date: payload.date,
    count: payload.contacts.length,
    time: `${pad(now.getHours())}:${pad(now.getMinutes())}`
  })

  dialogOpen.value = false
  emit('submit', payload)
  showToast(`提交成功，已通知 ${payload.contacts.length} 位家属`)

  // 回到干净的初始状态，保留所选日期
  contacts.value = [{ id: seq++, name: '', phone: '' }]
  note.value = ''
  errors.value = { date: false, items: {} }
  scrollEl.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

let toastTimer
function showToast(text) {
  toastText.value = text
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 2200)
}
</script>

<template>
  <div class="phone">
    <div class="statusbar">
      <span>9:41</span>
      <span class="icons">
        <span class="bar" style="height: 4px"></span>
        <span class="bar" style="height: 6px"></span>
        <span class="bar" style="height: 8px"></span>
        <span class="bar" style="height: 10px"></span>
        <span class="battery"></span>
      </span>
    </div>

    <header class="header">
      <div class="brand">
        <span class="logo">
          <svg viewBox="0 0 24 24" fill="none" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3l7 3v6c0 4.4-3 7.6-7 9-4-1.4-7-4.6-7-9V6l7-3z" />
            <path d="M9 12l2 2 4-4" />
          </svg>
        </span>
        平安托付
      </div>
      <p class="hint">仅在指定日期生效，提交后将自动通知预设家属</p>
    </header>

    <main ref="scrollEl" class="scroll">
      <section v-if="records.length" class="records">
        <div class="records-head">申请记录</div>
        <ul class="record-list">
          <li v-for="r in records" :key="r.id" class="record">
            <span class="record-check">
              <svg viewBox="0 0 24 24" fill="none" stroke-linecap="round" stroke-linejoin="round">
                <path d="M5 12.5l4.5 4.5L19 7" />
              </svg>
            </span>
            <div>
              <div class="record-title">已申请 · {{ shortDate(r.date) }}</div>
              <div class="record-sub">短信已发送至 {{ r.count }} 位家属 · {{ r.time }}</div>
            </div>
          </li>
        </ul>
      </section>

      <section class="card">
        <div class="row date-row" :class="{ invalid: errors.date }" @click="openPicker">
          <span class="row-label">生效日期</span>
          <span class="row-value">
            <span>{{ dateLabel }}</span>
            <svg class="chev" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 6l6 6-6 6" />
            </svg>
          </span>
          <input
            ref="dateInput"
            v-model="date"
            type="date"
            :min="today"
            aria-label="选择生效日期"
            @change="errors.date = false"
          />
        </div>

        <div class="block">
          <div class="block-head">
            <span class="block-title">短信通知家属</span>
            <span class="block-count">{{ contacts.length }} / {{ maxContacts }}</span>
          </div>

          <div class="contact-list">
            <div v-for="c in contacts" :key="c.id" class="contact-row">
              <input
                v-model="c.name"
                class="inp name"
                :class="{ invalid: errors.items[c.id]?.name }"
                placeholder="称呼"
                maxlength="8"
                :aria-label="`家属${c.name ? c.name : ''}称呼`"
                @input="clearFieldError(c.id, 'name')"
              />
              <input
                :value="c.phone"
                class="inp phone"
                :class="{ invalid: errors.items[c.id]?.phone }"
                placeholder="手机号码"
                inputmode="numeric"
                maxlength="11"
                aria-label="手机号码"
                @input="onPhoneInput(c, $event)"
              />
              <button
                v-if="contacts.length > 1"
                type="button"
                class="remove"
                aria-label="删除该家属"
                @click="removeContact(c.id)"
              >
                ×
              </button>
            </div>
          </div>

          <button v-if="canAdd" type="button" class="add" @click="addContact">
            <svg viewBox="0 0 24 24" fill="none" stroke-linecap="round"><path d="M12 5v14M5 12h14" /></svg>
            添加家属
          </button>

          <p class="sms-preview">
            【平安托付】您关注的家人已于 <b>{{ date ? shortDate(date) : '—' }}</b> 提交状态申请，请及时关注并与其联系。
          </p>
        </div>

        <details class="note">
          <summary>
            <span>备注</span><span class="opt">选填，可不填</span>
            <svg class="chev-down" viewBox="0 0 24 24" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 9l6 6 6-6" />
            </svg>
          </summary>
          <textarea v-model="note" maxlength="60" placeholder="简单交代一句，例如：如未回复请电话联系"></textarea>
        </details>
      </section>
    </main>

    <footer class="footer">
      <p v-if="formError" class="form-error show">{{ formError }}</p>
      <button class="cta" type="button" @click="submitRequest">确认提交申请</button>
    </footer>

    <div class="mask" :class="{ open: dialogOpen }" @click.self="closeDialog">
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="dlgTitle">
        <h3 id="dlgTitle">确认提交申请</h3>
        <template v-if="pending">
          <p>
            确认在 <b>{{ shortDate(pending.date) }}</b> 生效，并向以下 {{ pending.contacts.length }} 位家属发送短信提醒吗？
          </p>
          <div class="who">
            <template v-for="(c, i) in pending.contacts" :key="i">
              {{ c.name }}　<span>{{ c.phone }}</span><br v-if="i < pending.contacts.length - 1" />
            </template>
          </div>
        </template>
        <div class="dialog-actions">
          <button class="btn btn-ghost" type="button" @click="closeDialog">再想想</button>
          <button class="btn btn-primary" type="button" @click="confirmSubmit">确认并发送</button>
        </div>
      </div>
    </div>

    <div class="toast" :class="{ show: toastVisible }">{{ toastText }}</div>
  </div>
</template>

<style scoped>
.phone {
  --app-bg: #f7f8fa;
  --surface: #ffffff;
  --ink: #16211f;
  --ink-2: #3d4a47;
  --muted: #85928f;
  --line: #edf0ef;
  --primary: #0e6b5b;
  --primary-soft: #e9f4f1;
  --danger: #c1472f;

  position: relative;
  width: 390px;
  height: min(844px, calc(100vh - 40px));
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
  border-radius: 44px;
  box-shadow:
    0 0 0 10px #14201e,
    0 0 0 11px #2b3734,
    0 40px 80px rgba(14, 32, 30, 0.28);
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  color: var(--ink);
  text-align: left;
}
.phone *,
.phone *::before,
.phone *::after {
  box-sizing: border-box;
}

@media (max-width: 480px) {
  .phone {
    width: 100vw;
    height: 100vh;
    height: 100dvh;
    border-radius: 0;
    box-shadow: none;
  }
}

/* 状态栏 */
.statusbar {
  flex: none;
  height: 44px;
  padding: 0 26px 0 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--ink);
}
.statusbar .icons {
  display: flex;
  align-items: center;
  gap: 5px;
  opacity: 0.85;
}
.statusbar .icons .bar {
  width: 3px;
  background: var(--ink);
  border-radius: 1px;
}
.battery {
  width: 22px;
  height: 11px;
  border: 1.2px solid var(--ink);
  border-radius: 3px;
  position: relative;
  opacity: 0.9;
}
.battery::after {
  content: '';
  position: absolute;
  left: 1.4px;
  top: 1.4px;
  bottom: 1.4px;
  width: 13px;
  background: var(--ink);
  border-radius: 1.5px;
}
.battery::before {
  content: '';
  position: absolute;
  right: -3px;
  top: 3px;
  width: 1.6px;
  height: 4px;
  background: var(--ink);
  border-radius: 0 1px 1px 0;
}

/* 顶部 */
.header {
  flex: none;
  padding: 6px 24px 4px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.logo {
  width: 26px;
  height: 26px;
  border-radius: 9px;
  background: linear-gradient(140deg, #137a67, #0a5548);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(14, 107, 91, 0.28);
}
.logo svg {
  width: 15px;
  height: 15px;
  stroke: #fff;
}
.hint {
  margin: 9px 0 0;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--muted);
  letter-spacing: 0.1px;
}

/* 滚动区 */
.scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 20px 8px;
  scrollbar-width: none;
}
.scroll::-webkit-scrollbar {
  display: none;
}

/* 记录 */
.records {
  margin-bottom: 12px;
}
.records-head {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted);
  letter-spacing: 1px;
  margin: 0 4px 8px;
}
.record-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.record {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  background: var(--surface);
  border-radius: 16px;
  padding: 13px 14px;
  box-shadow: 0 2px 10px rgba(20, 40, 36, 0.05);
  animation: pop 0.32s cubic-bezier(0.22, 1.2, 0.4, 1);
}
@keyframes pop {
  from {
    opacity: 0;
    transform: translateY(-6px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: none;
  }
}
.record-check {
  flex: none;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 1px;
}
.record-check svg {
  width: 12px;
  height: 12px;
  stroke: var(--primary);
  stroke-width: 3;
}
.record-title {
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 0.2px;
}
.record-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 3px;
}

/* 主卡片 */
.card {
  background: var(--surface);
  border-radius: 20px;
  padding: 2px 18px 18px;
  box-shadow: 0 6px 24px rgba(20, 40, 36, 0.06);
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 58px;
  gap: 12px;
}
.row + .row,
.row + .block,
.block + .row {
  border-top: 1px solid var(--line);
}
.row-label {
  font-size: 14px;
  color: var(--ink-2);
  letter-spacing: 0.2px;
}
.row-value {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--ink);
  display: flex;
  align-items: center;
  gap: 4px;
}
.row-value .chev {
  width: 16px;
  height: 16px;
  stroke: #b3bebb;
  stroke-width: 2;
  fill: none;
}

/* 日期行 */
.date-row {
  position: relative;
  cursor: pointer;
  margin: 0 -18px;
  padding: 0 18px;
  border-radius: 14px;
  transition: background 0.18s;
}
.date-row:active {
  background: #f4f7f6;
}
.date-row:focus-within {
  box-shadow: inset 0 0 0 1.5px rgba(14, 107, 91, 0.35);
}
.date-row input[type='date'] {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  border: 0;
  padding: 0;
  margin: 0;
  cursor: pointer;
}
.date-row.invalid {
  box-shadow: inset 0 0 0 1.5px rgba(193, 71, 47, 0.55);
}

/* 家属区 */
.block {
  padding: 16px 0 4px;
}
.block-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 11px;
}
.block-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 0.2px;
}
.block-count {
  font-size: 11.5px;
  color: var(--muted);
}
.contact-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.contact-row {
  display: flex;
  align-items: center;
  gap: 8px;
  animation: pop 0.25s ease both;
}
.inp {
  height: 44px;
  border: 0;
  border-radius: 12px;
  background: #f5f7f7;
  font-family: inherit;
  font-size: 14px;
  color: var(--ink);
  padding: 0 12px;
  outline: none;
  transition: box-shadow 0.18s, background 0.18s;
}
.inp::placeholder {
  color: #a7b2af;
}
.inp:focus {
  background: #eff5f3;
  box-shadow: inset 0 0 0 1.5px rgba(14, 107, 91, 0.35);
}
.inp.name {
  flex: none;
  width: 86px;
  text-align: center;
}
.inp.phone {
  flex: 1;
  min-width: 0;
  letter-spacing: 0.4px;
}
.inp.invalid {
  box-shadow: inset 0 0 0 1.5px rgba(193, 71, 47, 0.5);
  background: #fdf4f2;
}
.remove {
  flex: none;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #b9c3c1;
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, color 0.15s;
}
.remove:active {
  background: #f2f5f4;
  color: var(--danger);
}
.add {
  width: 100%;
  height: 44px;
  margin-top: 9px;
  border: 1.2px dashed #cbd8d5;
  border-radius: 12px;
  background: transparent;
  color: var(--primary);
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 0.3px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: background 0.18s, border-color 0.18s;
}
.add:active {
  background: var(--primary-soft);
  border-color: #afcbc4;
}
.add svg {
  width: 13px;
  height: 13px;
  stroke: currentColor;
  stroke-width: 2.4;
}
.sms-preview {
  margin: 12px 0 0;
  padding: 11px 13px;
  background: var(--primary-soft);
  border-radius: 12px;
  font-size: 11.5px;
  line-height: 1.75;
  color: #3a6e64;
  letter-spacing: 0.15px;
}
.sms-preview b {
  font-weight: 600;
}

/* 备注 */
.note {
  border-top: 1px solid var(--line);
  margin-top: 14px;
  padding-top: 2px;
}
.note summary {
  list-style: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 46px;
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
}
.note summary::-webkit-details-marker {
  display: none;
}
.note summary .opt {
  font-size: 11.5px;
  color: var(--muted);
  margin-right: auto;
  margin-left: 7px;
}
.note summary .chev-down {
  width: 14px;
  height: 14px;
  stroke: #b3bebb;
  stroke-width: 2;
  fill: none;
  transition: transform 0.2s;
}
.note[open] summary .chev-down {
  transform: rotate(180deg);
}
.note textarea {
  width: 100%;
  height: 66px;
  border: 0;
  border-radius: 12px;
  background: #f5f7f7;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ink);
  padding: 11px 12px;
  outline: none;
  resize: none;
  margin-bottom: 14px;
}
.note textarea:focus {
  box-shadow: inset 0 0 0 1.5px rgba(14, 107, 91, 0.35);
  background: #eff5f3;
}
.note textarea::placeholder {
  color: #a7b2af;
}

/* 底部 */
.footer {
  flex: none;
  padding: 8px 20px calc(18px + env(safe-area-inset-bottom));
  background: linear-gradient(to top, var(--app-bg) 62%, rgba(247, 248, 250, 0));
}
.form-error {
  display: none;
  margin: 0 2px 8px;
  font-size: 12px;
  color: var(--danger);
  letter-spacing: 0.2px;
}
.form-error.show {
  display: block;
  animation: pop 0.2s ease;
}
.cta {
  width: 100%;
  height: 52px;
  border: 0;
  border-radius: 16px;
  background: linear-gradient(135deg, #127867, #0a5548);
  color: #fff;
  font-family: inherit;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1.5px;
  cursor: pointer;
  box-shadow: 0 10px 22px rgba(14, 107, 91, 0.26);
  transition: transform 0.12s, box-shadow 0.12s, opacity 0.2s;
}
.cta:active {
  transform: scale(0.985);
  box-shadow: 0 5px 14px rgba(14, 107, 91, 0.22);
}

/* 弹窗 */
.mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  background: rgba(10, 22, 20, 0.44);
  -webkit-backdrop-filter: blur(3px);
  backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 26px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.22s;
}
.mask.open {
  opacity: 1;
  pointer-events: auto;
}
.dialog {
  width: 100%;
  background: #fff;
  border-radius: 22px;
  padding: 24px 22px 18px;
  transform: translateY(10px) scale(0.96);
  opacity: 0;
  transition: transform 0.26s cubic-bezier(0.2, 0.9, 0.3, 1.15), opacity 0.2s;
}
.mask.open .dialog {
  transform: none;
  opacity: 1;
}
.dialog h3 {
  margin: 0 0 10px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.4px;
}
.dialog p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ink-2);
}
.dialog .who {
  margin-top: 12px;
  padding: 11px 13px;
  border-radius: 12px;
  background: #f5f7f7;
  font-size: 12.5px;
  color: var(--ink-2);
  line-height: 1.7;
}
.dialog .who span {
  color: var(--muted);
}
.dialog-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}
.btn {
  flex: 1;
  height: 46px;
  border: 0;
  border-radius: 14px;
  font-family: inherit;
  font-size: 14.5px;
  font-weight: 600;
  letter-spacing: 0.6px;
  cursor: pointer;
  transition: transform 0.12s, background 0.15s;
}
.btn:active {
  transform: scale(0.98);
}
.btn-ghost {
  background: #f2f4f4;
  color: var(--ink-2);
}
.btn-primary {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 6px 16px rgba(14, 107, 91, 0.24);
}

/* Toast */
.toast {
  position: absolute;
  left: 50%;
  bottom: 104px;
  z-index: 30;
  transform: translate(-50%, 8px);
  opacity: 0;
  pointer-events: none;
  background: rgba(22, 33, 31, 0.92);
  color: #fff;
  font-size: 12.5px;
  letter-spacing: 0.3px;
  padding: 10px 16px;
  border-radius: 99px;
  white-space: nowrap;
  transition: opacity 0.22s, transform 0.22s;
}
.toast.show {
  opacity: 1;
  transform: translate(-50%, 0);
}
</style>
