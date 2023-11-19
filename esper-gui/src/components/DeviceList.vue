<script setup lang="ts">
export interface DeviceProps {
	id: string,
	url: string,
	name: string,
	nameByUser?: string,
	model: string,
	manufacturer: string,
	version?: string,
	newVersion?: string
}
defineProps<{
	devices: DeviceProps[]
}>()

defineEmits(['updateFirmware'])
</script>

<template>
    <div class="wrapper">
		<ui-list :type="2">
			<ui-item v-for="device in devices" :key="device">
				<ui-item-text-content class="item-details">
					<ui-item-text1>{{ device.nameByUser ?? device.name }}</ui-item-text1>
					<ui-item-text2>{{ device.manufacturer }} / {{ device.model }}: {{ device.version ?? "Unknown version" }}</ui-item-text2>
				</ui-item-text-content>
				<ui-item-last-content v-if="device.newVersion">
					<ui-fab extended mini @click="$emit('updateFirmware', device)" class="button">
						<span>{{ device.newVersion }}</span>
						<template #before="{ iconClass }">
							<ui-icon :class="iconClass">upload</ui-icon>
						</template>
					</ui-fab>
				</ui-item-last-content>
			</ui-item>
		</ui-list>
	</div>
</template>

<style>
.button {
	text-transform: none !important;
}
.item-details {
	margin-right: 25px;
}
</style>