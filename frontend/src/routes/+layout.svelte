<script lang="ts">
	import './layout.css';
	import NintendoDSFrame from '$lib/components/NintendoDSFrame.svelte';

	
	let { children } = $props();
</script>

<NintendoDSFrame>
	{@render children()}
</NintendoDSFrame>
